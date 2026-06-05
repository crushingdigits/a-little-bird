package dev.crushingdigits.littlebird.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.GameStatus
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWall
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatched
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAt
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.ObserveSchedule
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
import dev.crushingdigits.littlebird.domain.usecase.SyncSchedule
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeFeed: ObserveFeed,
    private val observeProtection: ObserveProtection,
    private val observeRevealed: ObserveRevealed,
    private val refreshFeed: RefreshFeed,
    private val toggleProtection: ToggleProtection,
    private val revealItem: RevealItem,
    private val filterFeed: FilterFeed,
    private val observeFilterPrefs: ObserveFilterPrefs,
    private val setReliabilityLevel: SetReliabilityLevel,
    private val setPinnedFeed: SetPinnedFeed,
    private val observeSchedule: ObserveSchedule,
    private val observeWatchedGames: ObserveWatchedGames,
    private val observeLastSyncAt: ObserveLastSyncAt,
    private val markGameWatched: MarkGameWatched,
    private val restoreWatchedGames: RestoreWatchedGames,
    private val syncSchedule: SyncSchedule,
    private val computeSpoilerWall: ComputeSpoilerWall,
    private val clock: Clock,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val offline = MutableStateFlow(false)
    private val gateTarget = MutableStateFlow<NewsItem?>(null)
    private val wallSheetVisible = MutableStateFlow(false)

    // Session-only selected feed; re-seeded from the pinned feed on cold launch.
    private val selectedType = MutableStateFlow(FeedType.ALL)

    private val _actions = MutableSharedFlow<FeedAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<FeedAction> = _actions.asSharedFlow()

    init {
        viewModelScope.launch {
            selectedType.value = observeFilterPrefs().first().pinned ?: FeedType.ALL
        }
    }

    private data class Transient(val refreshing: Boolean, val offline: Boolean, val gate: NewsItem?, val sheet: Boolean)
    private val transient = combine(refreshing, offline, gateTarget, wallSheetVisible, ::Transient)

    private data class FilterState(val type: FeedType, val maxTier: Tier, val pinned: FeedType?)
    private val filterState = combine(selectedType, observeFilterPrefs()) { type, prefs ->
        FilterState(type, prefs.maxTier, prefs.pinned)
    }

    private data class Spoiler(
        val wall: Instant?,
        val revealed: Set<String>,
        val hidingCount: Int,
        val fixtures: List<FixtureUi>,
        val lastSyncAt: Instant?,
    )

    private val spoiler = combine(
        observeSchedule(), observeWatchedGames(), observeRevealed(), observeLastSyncAt(),
    ) { games, watched, revealed, lastSyncMs ->
        val now = clock.instant()
        val wall = computeSpoilerWall(games, watched, now)
        val hidingCount = games.count { it.status(now) == GameStatus.PLAYED && it.id !in watched }
        Spoiler(
            wall = wall,
            revealed = revealed,
            hidingCount = hidingCount,
            fixtures = games.toFixtureUi(watched, now, wall),
            lastSyncAt = lastSyncMs?.let(Instant::ofEpochMilli),
        )
    }

    val uiState: StateFlow<FeedUiState> = combine(
        observeFeed(), observeProtection(), filterState, transient, spoiler,
    ) { items, protectionOn, fs, t, sp ->
        val filtered = filterFeed(items, FeedFilter(fs.type, fs.maxTier))
        val cards = filtered.visible.map { item ->
            if (isHidden(item, protectionOn, sp.wall, sp.revealed)) FeedCard.Redacted(item) else FeedCard.Normal(item)
        }
        FeedUiState(
            cards = cards,
            protectionOn = protectionOn,
            hidingCount = sp.hidingCount,
            isRefreshing = t.refreshing,
            isOffline = t.offline,
            isEmpty = items.isEmpty() && !t.refreshing,
            gateTarget = t.gate,
            wallSheetVisible = t.sheet,
            fixtures = sp.fixtures,
            lastSyncAt = sp.lastSyncAt,
            filterTabs = FeedType.entries.map { FeedTab(it, filtered.counts[it] ?: 0) },
            selectedType = fs.type,
            reliabilityLevel = fs.maxTier,
            pinnedType = fs.pinned,
            isFilteredEmpty = items.isNotEmpty() && filtered.visible.isEmpty() && !t.refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    private fun isHidden(item: NewsItem, protectionOn: Boolean, wall: Instant?, revealed: Set<String>): Boolean =
        protectionOn && wall != null && !item.publishedAt.isBefore(wall) && item.id !in revealed

    fun onToggleProtection() = viewModelScope.launch { toggleProtection() }

    fun onCardClick(card: FeedCard) {
        when (card) {
            is FeedCard.Redacted -> gateTarget.value = card.item
            is FeedCard.Normal -> navigateToDetail(card.item.id)
        }
    }

    fun onDismissGate() { gateTarget.value = null }

    fun onRevealFromGate(id: String) = viewModelScope.launch {
        revealItem(id)
        gateTarget.value = null
        _actions.emit(FeedAction.NavigateToDetail(id))
    }

    private fun navigateToDetail(id: String) = viewModelScope.launch {
        _actions.emit(FeedAction.NavigateToDetail(id))
    }

    fun onOpenWallSheet() { wallSheetVisible.value = true }
    fun onDismissWallSheet() { wallSheetVisible.value = false }

    fun onMarkGameWatched(gameId: String) = viewModelScope.launch {
        val before = currentWall()
        val snapshot = markGameWatched(gameId, watched = true)
        val after = currentWall()
        val unlocked = countUnlocked(before, after)
        _actions.emit(
            FeedAction.ShowToast(
                message = "$unlocked ${if (unlocked == 1) "story" else "stories"} unlocked",
                undoSnapshot = snapshot,
            ),
        )
    }

    fun onUnmarkGame(gameId: String) = viewModelScope.launch { markGameWatched(gameId, watched = false) }

    fun onUndo(snapshot: Set<String>) = viewModelScope.launch { restoreWatchedGames(snapshot) }

    private suspend fun currentWall(): Instant? =
        computeSpoilerWall(observeSchedule().first(), observeWatchedGames().first(), clock.instant())

    /** Feed items released by advancing the wall from [before] up to (but excluding) [after]. */
    private suspend fun countUnlocked(before: Instant?, after: Instant?): Int {
        if (before == null) return 0
        val revealed = observeRevealed().first()
        return observeFeed().first().count { item ->
            !item.publishedAt.isBefore(before) &&
                (after == null || item.publishedAt.isBefore(after)) &&
                item.id !in revealed
        }
    }

    fun onRefresh() = viewModelScope.launch {
        refreshing.value = true
        val feed = refreshFeed()
        syncSchedule()
        offline.value = feed.isFailure
        refreshing.value = false
    }

    fun onRefreshSchedule() = viewModelScope.launch {
        offline.value = syncSchedule().isFailure
    }

    // ---- filter handlers ----
    fun onSelectType(type: FeedType) { selectedType.value = type }
    fun onSetReliability(maxTier: Tier) = viewModelScope.launch { setReliabilityLevel(maxTier) }
    fun onResetReliability() = viewModelScope.launch { setReliabilityLevel(Tier.TIER_3_SPECULATIVE) }
    fun onTogglePin() = viewModelScope.launch {
        val current = selectedType.value
        val pinned = observeFilterPrefs().first().pinned
        setPinnedFeed(if (pinned == current) null else current)
    }
}
