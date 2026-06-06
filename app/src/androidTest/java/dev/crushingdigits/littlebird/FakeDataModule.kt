package dev.crushingdigits.littlebird

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.di.DataBindingsModule
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Singleton

class FakeNewsRepository : NewsRepository {
    private val items = MutableStateFlow(
        listOf(
            NewsItem(
                id = "match-1", source = "BBC Sport", tier = Tier.TIER_2_BEAT, category = Category.MATCH,
                isMatch = true, confirmed = false, author = null, role = null,
                title = "Match result hidden under spoiler protection", summary = "Full-time report.",
                link = "https://x/m1", imageUrl = null,
                publishedAt = Instant.now().minusSeconds(3_600), // ~1h ago: after the wall -> hidden
            ),
            NewsItem(
                id = "transfer-1", source = "Fabrizio Romano", tier = Tier.TIER_1_RELIABLE, category = Category.TRANSFER,
                isMatch = false, confirmed = true, author = null, role = null,
                title = "Liverpool agree £65m deal for midfielder", summary = "Here we go.",
                link = "https://x/t1", imageUrl = null,
                publishedAt = Instant.now().minusSeconds(2 * 24 * 3_600), // ~2 days ago: before the wall -> visible
            ),
        ),
    )
    override fun observeFeed(): Flow<List<NewsItem>> = items
    override fun observeItem(id: String): Flow<NewsItem?> = items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    override suspend fun isEmpty(): Boolean = false
}

class FakeScheduleRepository : ScheduleRepository {
    // One game played ~1 day ago: PLAYED (so it forms the wall) but WITHIN the 3-day seed window, so
    // SyncSchedule's first-run seeding does NOT auto-mark it watched. Timestamps are relative to now
    // because the production Clock is the real system clock. -> wall = ~1 day ago; the match item
    // (~1h ago) is hidden, the transfer item (~2 days ago) is visible.
    private val games = MutableStateFlow(
        listOf(
            Game(
                id = "g1", opponent = "Chelsea", abbreviation = "CHE", isHome = true, competition = "English Premier League",
                kickoff = Instant.now().minusSeconds(24 * 3_600), homeScore = 2, awayScore = 1,
            ),
        ),
    )
    override fun observeSchedule(): Flow<List<Game>> = games
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}

class FakePreferencesRepository : PreferencesRepository {
    private val protection = MutableStateFlow(true)
    private val revealed = MutableStateFlow<Set<String>>(emptySet())
    private val reliabilityLevel = MutableStateFlow(3)
    private val pinnedFeed = MutableStateFlow<String?>(null)
    private val watched = MutableStateFlow<Set<String>>(emptySet())
    private val lastSync = MutableStateFlow<Long?>(null)
    private var firstSync: Long? = null

    override fun observeProtection(): Flow<Boolean> = protection
    override fun observeRevealed(): Flow<Set<String>> = revealed
    override suspend fun setProtection(on: Boolean) { protection.value = on }
    override suspend fun toggleProtection() { protection.value = !protection.value }
    override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
    override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
    override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
    override fun observeReliabilityLevel(): Flow<Int> = reliabilityLevel
    override fun observePinnedFeed(): Flow<String?> = pinnedFeed
    override suspend fun setReliabilityLevel(level: Int) { reliabilityLevel.value = level }
    override suspend fun setPinnedFeed(key: String?) { pinnedFeed.value = key }
    override fun observeWatchedGames(): Flow<Set<String>> = watched
    override suspend fun markGameWatched(id: String) { watched.value = watched.value + id }
    override suspend fun unmarkGameWatched(id: String) { watched.value = watched.value - id }
    override suspend fun setWatchedGames(ids: Set<String>) { watched.value = ids }
    override suspend fun getFirstScheduleSyncAt(): Long? = firstSync
    override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
    override fun observeLastScheduleSyncAt(): Flow<Long?> = lastSync
    override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataBindingsModule::class])
object FakeDataModule {
    @Provides @Singleton fun newsRepository(): NewsRepository = FakeNewsRepository()
    @Provides @Singleton fun scheduleRepository(): ScheduleRepository = FakeScheduleRepository()
    @Provides @Singleton fun preferencesRepository(): PreferencesRepository = FakePreferencesRepository()
}
