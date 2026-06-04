package dev.crushingdigits.littlebird.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.MarkWatched
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealed
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
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
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeFeed: ObserveFeed,
    private val observeProtection: ObserveProtection,
    private val observeRevealed: ObserveRevealed,
    private val refreshFeed: RefreshFeed,
    private val toggleProtection: ToggleProtection,
    private val revealItem: RevealItem,
    private val markWatched: MarkWatched,
    private val restoreRevealed: RestoreRevealed,
    private val filterFeed: FilterFeed,
    private val observeFilterPrefs: ObserveFilterPrefs,
    private val setReliabilityLevel: SetReliabilityLevel,
    private val setPinnedFeed: SetPinnedFeed,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val offline = MutableStateFlow(false)
    private val gateTarget = MutableStateFlow<NewsItem?>(null)
    private val catchUpVisible = MutableStateFlow(false)

    // Session-only selected feed; re-seeded from the pinned feed on cold launch.
    private val selectedType = MutableStateFlow(FeedType.ALL)

    private val _actions = MutableSharedFlow<FeedAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<FeedAction> = _actions.asSharedFlow()

    init {
        viewModelScope.launch {
            selectedType.value = observeFilterPrefs().first().pinned ?: FeedType.ALL
        }
    }

    private data class Transient(val refreshing: Boolean, val offline: Boolean, val gate: NewsItem?, val catchUp: Boolean)
    private val transient = combine(refreshing, offline, gateTarget, catchUpVisible, ::Transient)

    private data class FilterState(val type: FeedType, val maxTier: Tier, val pinned: FeedType?)
    private val filterState = combine(selectedType, observeFilterPrefs()) { type, prefs ->
        FilterState(type, prefs.maxTier, prefs.pinned)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        observeFeed(), observeProtection(), observeRevealed(), filterState, transient,
    ) { items, protectionOn, revealed, fs, t ->
        val filtered = filterFeed(items, FeedFilter(fs.type, fs.maxTier))
        val cards = filtered.visible.map { item ->
            if (protectionOn && item.isMatch && item.id !in revealed) FeedCard.Redacted(item) else FeedCard.Normal(item)
        }
        // Held count is GLOBAL (whole feed), so catch-up is consistent across tabs.
        val held = items.count { protectionOn && it.isMatch && it.id !in revealed }
        FeedUiState(
            cards = cards,
            protectionOn = protectionOn,
            heldMatchCount = held,
            isRefreshing = t.refreshing,
            isOffline = t.offline,
            isEmpty = items.isEmpty() && !t.refreshing,
            gateTarget = t.gate,
            catchUpVisible = t.catchUp,
            filterTabs = FeedType.entries.map { FeedTab(it, filtered.counts[it] ?: 0) },
            selectedType = fs.type,
            reliabilityLevel = fs.maxTier,
            pinnedType = fs.pinned,
            isFilteredEmpty = items.isNotEmpty() && filtered.visible.isEmpty() && !t.refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

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
    fun onRequestCatchUp() { catchUpVisible.value = true }
    fun onDismissCatchUp() { catchUpVisible.value = false }

    fun onMarkWatched() = viewModelScope.launch {
        // Derive heldIds directly from the source flows rather than from uiState.value.
        // uiState is a WhileSubscribed(5000) StateFlow — when there are no active collectors
        // it returns the stale initial FeedUiState() (empty cards). Reading from the upstream
        // flows via .first() always returns the current truth regardless of subscriber count.
        val items = observeFeed().first()
        val protectionOn = observeProtection().first()
        val revealed = observeRevealed().first()
        val heldIds = items
            .filter { protectionOn && it.isMatch && it.id !in revealed }
            .map { it.id }
            .toSet()
        val snapshot = markWatched(heldIds)
        catchUpVisible.value = false
        _actions.emit(FeedAction.ShowToast("${heldIds.size} match updates unlocked", undoSnapshot = snapshot))
    }

    fun onUndo(snapshot: Set<String>) = viewModelScope.launch { restoreRevealed(snapshot) }

    fun onRefresh() = viewModelScope.launch {
        refreshing.value = true
        val result = refreshFeed()
        offline.value = result.isFailure
        refreshing.value = false
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
