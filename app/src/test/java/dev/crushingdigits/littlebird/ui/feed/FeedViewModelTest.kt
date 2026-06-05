package dev.crushingdigits.littlebird.ui.feed

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.MainDispatcherRule
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.MarkWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtectionUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RevealItemUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class FeedViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun item(id: String, match: Boolean) = NewsItem(
        id = id, source = "BBC", tier = Tier.TIER_2_BEAT, category = if (match) Category.MATCH else Category.TRANSFER,
        isMatch = match, confirmed = false, author = null, role = null, title = "t$id", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private fun item(id: String, category: Category, tier: Tier = Tier.TIER_1_RELIABLE) = NewsItem(
        id = id, source = "BBC", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t$id", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private class FakeNews(items: List<NewsItem>) : NewsRepository {
        val flow = MutableStateFlow(items)
        override fun observeFeed(): Flow<List<NewsItem>> = flow
        override fun observeItem(id: String): Flow<NewsItem?> = flow.map { items -> items.firstOrNull { it.id == id } }
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun isEmpty(): Boolean = flow.value.isEmpty()
    }
    private class FakePrefs(on: Boolean, pinned: String? = null, level: Int = 3) : PreferencesRepository {
        val protection = MutableStateFlow(on)
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        val reliabilityLevel = MutableStateFlow(level)
        val pinnedFeed = MutableStateFlow(pinned)
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
        private val watchedGames = MutableStateFlow<Set<String>>(emptySet())
        private val lastSync = MutableStateFlow<Long?>(null)
        private var firstSync: Long? = null
        override fun observeWatchedGames(): Flow<Set<String>> = watchedGames
        override suspend fun markGameWatched(id: String) { watchedGames.value = watchedGames.value + id }
        override suspend fun unmarkGameWatched(id: String) { watchedGames.value = watchedGames.value - id }
        override suspend fun setWatchedGames(ids: Set<String>) { watchedGames.value = ids }
        override suspend fun getFirstScheduleSyncAt(): Long? = firstSync
        override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
        override fun observeLastScheduleSyncAt(): Flow<Long?> = lastSync
        override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
    }

    private fun vm(news: FakeNews, prefs: FakePrefs) = FeedViewModel(
        observeFeed = ObserveFeedUseCase(news),
        observeProtection = ObserveProtectionUseCase(prefs),
        observeRevealed = ObserveRevealedUseCase(prefs),
        refreshFeed = RefreshFeedUseCase(news),
        toggleProtection = ToggleProtectionUseCase(prefs),
        revealItem = RevealItemUseCase(prefs),
        markWatched = MarkWatchedUseCase(prefs),
        restoreRevealed = RestoreRevealedUseCase(prefs),
        filterFeed = FilterFeedUseCase(),
        observeFilterPrefs = ObserveFilterPrefsUseCase(prefs),
        setReliabilityLevel = SetReliabilityLevelUseCase(prefs),
        setPinnedFeed = SetPinnedFeedUseCase(prefs),
    )

    @Test fun `protection off renders everything Normal`() = runTest {
        val sut = vm(FakeNews(listOf(item("a", true), item("b", false))), FakePrefs(on = false))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.all { it is FeedCard.Normal }).isTrue()
            assertThat(s.heldMatchCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `protection on redacts unrevealed match items and counts held`() = runTest {
        val sut = vm(FakeNews(listOf(item("m", true), item("t", false))), FakePrefs(on = true))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.first { it.item.id == "m" }).isInstanceOf(FeedCard.Redacted::class.java)
            assertThat(s.cards.first { it.item.id == "t" }).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.heldMatchCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `revealing a held item turns it Normal and clears the gate`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("m", true))), prefs)
        sut.onCardClick(FeedCard.Redacted(item("m", true)))
        sut.onRevealFromGate("m")
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.single()).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.gateTarget).isNull()
            assertThat(s.heldMatchCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `markWatched reveals all held and emits an undo toast`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("m1", true), item("m2", true))), prefs)
        sut.actions.test {
            sut.onMarkWatched()
            val action = awaitItem()
            assertThat(action).isInstanceOf(FeedAction.ShowToast::class.java)
            assertThat((action as FeedAction.ShowToast).undoSnapshot).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.revealed.value).containsExactly("m1", "m2")
    }

    @Test fun `clicking a normal card emits NavigateToDetail`() = runTest {
        val sut = vm(FakeNews(listOf(item("t", false))), FakePrefs(on = false))
        sut.actions.test {
            sut.onCardClick(FeedCard.Normal(item("t", false)))
            assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("t"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `clicking a redacted card opens the gate and does not navigate`() = runTest {
        val sut = vm(FakeNews(listOf(item("m", true))), FakePrefs(on = true))
        sut.onCardClick(FeedCard.Redacted(item("m", true)))
        sut.uiState.test {
            assertThat(awaitItem().gateTarget?.id).isEqualTo("m")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `revealing from the gate navigates to detail`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("m", true))), prefs)
        sut.actions.test {
            sut.onRevealFromGate("m")
            assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("m"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.revealed.value).contains("m")
    }

    @Test fun `selecting a type filters the visible cards`() = runTest {
        val news = FakeNews(listOf(item("t", Category.TRANSFER), item("m", Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t")
            assertThat(s.selectedType).isEqualTo(FeedType.TRANSFERS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `lowering reliability hides higher-tier items and updates counts`() = runTest {
        val news = FakeNews(
            listOf(
                item("t1", Category.TRANSFER, Tier.TIER_1_RELIABLE),
                item("t3", Category.TRANSFER, Tier.TIER_3_SPECULATIVE),
            ),
        )
        val sut = vm(news, FakePrefs(on = false))
        sut.onSetReliability(Tier.TIER_1_RELIABLE)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t1")
            assertThat(s.filterTabs.first { it.type == FeedType.ALL }.count).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggling pin pins then unpins the current feed`() = runTest {
        val prefs = FakePrefs(on = false)
        val sut = vm(FakeNews(listOf(item("t", Category.TRANSFER))), prefs)
        sut.onSelectType(FeedType.TRANSFERS)
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isEqualTo("transfer")
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isNull()
    }

    @Test fun `isFilteredEmpty is set when the feed has items but none match`() = runTest {
        val sut = vm(FakeNews(listOf(item("t", Category.TRANSFER))), FakePrefs(on = false))
        sut.onSelectType(FeedType.MATCH)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards).isEmpty()
            assertThat(s.isFilteredEmpty).isTrue()
            assertThat(s.isEmpty).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `cold launch seeds the selected type from the pinned feed`() = runTest {
        val news = FakeNews(listOf(item("t", Category.TRANSFER), item("m", Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false, pinned = "match"))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.selectedType).isEqualTo(FeedType.MATCH)
            assertThat(s.cards.map { it.item.id }).containsExactly("m")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
