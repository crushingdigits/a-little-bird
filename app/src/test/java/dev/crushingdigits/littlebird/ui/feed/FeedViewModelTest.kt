package dev.crushingdigits.littlebird.ui.feed

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.MainDispatcherRule
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallUseCase
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAtUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtectionUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RevealItemUseCase
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
import dev.crushingdigits.littlebird.domain.usecase.SyncScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FeedViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val beforeWall = Instant.parse("2026-05-30T00:00:00Z") // before any game -> visible
    private val afterWall = Instant.parse("2026-06-02T00:00:00Z")  // after the wall game -> hidden

    private fun item(id: String, at: Instant = afterWall, category: Category = Category.TRANSFER, tier: Tier = Tier.TIER_1_RELIABLE) = NewsItem(
        id = id, source = "BBC", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t$id", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = at,
    )

    private fun game(id: String, ko: String, watchedScore: Pair<Int, Int>? = null) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true, competition = "PL",
        kickoff = Instant.parse(ko), homeScore = watchedScore?.first, awayScore = watchedScore?.second,
    )

    private val wallGame = game("g1", "2026-06-01T16:00:00Z", watchedScore = 2 to 1) // played, unwatched -> wall

    private class FakeNews(items: List<NewsItem>) : NewsRepository {
        val flow = MutableStateFlow(items)
        override fun observeFeed(): Flow<List<NewsItem>> = flow
        override fun observeItem(id: String): Flow<NewsItem?> = flow.map { l -> l.firstOrNull { it.id == id } }
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun isEmpty(): Boolean = flow.value.isEmpty()
    }

    private class FakeSchedule(games: List<Game>) : ScheduleRepository {
        val flow = MutableStateFlow(games)
        override fun observeSchedule(): Flow<List<Game>> = flow
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private class FakePrefs(on: Boolean, pinned: String? = null, level: Int = 3, watchedGames: Set<String> = emptySet()) : PreferencesRepository {
        val protection = MutableStateFlow(on)
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        val reliabilityLevel = MutableStateFlow(level)
        val pinnedFeed = MutableStateFlow(pinned)
        val watched = MutableStateFlow(watchedGames)
        val lastSync = MutableStateFlow<Long?>(null)
        var firstSync: Long? = null
        override fun observeProtection() = protection
        override fun observeRevealed() = revealed
        override suspend fun setProtection(on: Boolean) { protection.value = on }
        override suspend fun toggleProtection() { protection.value = !protection.value }
        override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
        override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
        override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
        override fun observeReliabilityLevel() = reliabilityLevel
        override fun observePinnedFeed() = pinnedFeed
        override suspend fun setReliabilityLevel(level: Int) { reliabilityLevel.value = level }
        override suspend fun setPinnedFeed(key: String?) { pinnedFeed.value = key }
        override fun observeWatchedGames() = watched
        override suspend fun markGameWatched(id: String) { watched.value = watched.value + id }
        override suspend fun unmarkGameWatched(id: String) { watched.value = watched.value - id }
        override suspend fun setWatchedGames(ids: Set<String>) { watched.value = ids }
        override suspend fun getFirstScheduleSyncAt() = firstSync
        override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
        override fun observeLastScheduleSyncAt() = lastSync
        override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
    }

    private fun vm(news: FakeNews, prefs: FakePrefs, schedule: FakeSchedule) = FeedViewModel(
        observeFeed = ObserveFeedUseCase(news),
        observeProtection = ObserveProtectionUseCase(prefs),
        observeRevealed = ObserveRevealedUseCase(prefs),
        refreshFeed = RefreshFeedUseCase(news),
        toggleProtection = ToggleProtectionUseCase(prefs),
        revealItem = RevealItemUseCase(prefs),
        filterFeed = FilterFeedUseCase(),
        observeFilterPrefs = ObserveFilterPrefsUseCase(prefs),
        setReliabilityLevel = SetReliabilityLevelUseCase(prefs),
        setPinnedFeed = SetPinnedFeedUseCase(prefs),
        observeSchedule = ObserveScheduleUseCase(schedule),
        observeWatchedGames = ObserveWatchedGamesUseCase(prefs),
        observeLastSyncAt = ObserveLastSyncAtUseCase(prefs),
        markGameWatched = MarkGameWatchedUseCase(prefs),
        restoreWatchedGames = RestoreWatchedGamesUseCase(prefs),
        syncSchedule = SyncScheduleUseCase(schedule, prefs, SeedWatchedOnFirstSyncUseCase(), clock),
        computeSpoilerWall = ComputeSpoilerWallUseCase(),
        clock = clock,
    )

    @Test fun `protection off renders everything Normal`() = runTest {
        val sut = vm(FakeNews(listOf(item("a", afterWall), item("b", afterWall))), FakePrefs(on = false), FakeSchedule(listOf(wallGame)))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.all { it is FeedCard.Normal }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `items after the wall are redacted, items before are normal`() = runTest {
        val sut = vm(
            FakeNews(listOf(item("after", afterWall), item("before", beforeWall))),
            FakePrefs(on = true),
            FakeSchedule(listOf(wallGame)),
        )
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.first { it.item.id == "after" }).isInstanceOf(FeedCard.Redacted::class.java)
            assertThat(s.cards.first { it.item.id == "before" }).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.hidingCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `no wall means nothing is hidden`() = runTest {
        // The only game is upcoming -> no wall -> all visible even with protection on.
        val sut = vm(FakeNews(listOf(item("x", afterWall))), FakePrefs(on = true), FakeSchedule(listOf(game("u", "2026-06-20T16:00:00Z"))))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.single()).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.hidingCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `marking the wall game watched reveals its stories and emits an undo toast`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.actions.test {
            sut.onMarkGameWatched("g1")
            val action = awaitItem()
            assertThat(action).isInstanceOf(FeedAction.ShowToast::class.java)
            assertThat((action as FeedAction.ShowToast).undoSnapshot).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.watched.value).containsExactly("g1")
    }

    @Test fun `undo restores the prior watched set`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.onMarkGameWatched("g1")
        sut.onUndo(emptySet())
        assertThat(prefs.watched.value).isEmpty()
    }

    @Test fun `fixtures expose a score only for watched games`() = runTest {
        val prefs = FakePrefs(on = true, watchedGames = setOf("g1"))
        val sut = vm(FakeNews(emptyList()), prefs, FakeSchedule(listOf(wallGame, game("g2", "2026-06-03T16:00:00Z", 0 to 0))))
        sut.uiState.test {
            val s = awaitItem()
            val watched = s.fixtures.first { it.id == "g1" }
            val unwatched = s.fixtures.first { it.id == "g2" }
            assertThat(watched.scoreLine).isEqualTo("2–1")
            assertThat(unwatched.scoreLine).isNull()
            assertThat(unwatched.isWall).isTrue() // g2 is now the earliest unwatched played game
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `revealing a hidden item from the gate turns it Normal and navigates`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.actions.test {
            sut.onRevealFromGate("after")
            assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("after"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.revealed.value).contains("after")
    }

    @Test fun `clicking a redacted card opens the gate`() = runTest {
        val sut = vm(FakeNews(listOf(item("after", afterWall))), FakePrefs(on = true), FakeSchedule(listOf(wallGame)))
        sut.onCardClick(FeedCard.Redacted(item("after", afterWall)))
        sut.uiState.test {
            assertThat(awaitItem().gateTarget?.id).isEqualTo("after")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `selecting a type filters the visible cards`() = runTest {
        val news = FakeNews(listOf(item("t", beforeWall, Category.TRANSFER), item("m", beforeWall, Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false), FakeSchedule(listOf(wallGame)))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t")
            assertThat(s.selectedType).isEqualTo(FeedType.TRANSFERS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggling pin pins then unpins the current feed`() = runTest {
        val prefs = FakePrefs(on = false)
        val sut = vm(FakeNews(listOf(item("t", beforeWall, Category.TRANSFER))), prefs, FakeSchedule(emptyList()))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isEqualTo("transfer")
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isNull()
    }

    @Test fun `cold launch seeds the selected type from the pinned feed`() = runTest {
        val news = FakeNews(listOf(item("t", beforeWall, Category.TRANSFER), item("m", beforeWall, Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false, pinned = "match"), FakeSchedule(emptyList()))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.selectedType).isEqualTo(FeedType.MATCH)
            assertThat(s.cards.map { it.item.id }).containsExactly("m")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
