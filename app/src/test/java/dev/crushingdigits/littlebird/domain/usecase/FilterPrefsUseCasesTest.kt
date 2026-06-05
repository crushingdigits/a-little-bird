package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakePrefs : PreferencesRepository {
    val level = MutableStateFlow(3)
    val pinned = MutableStateFlow<String?>(null)
    override fun observeProtection(): Flow<Boolean> = MutableStateFlow(true)
    override fun observeRevealed(): Flow<Set<String>> = MutableStateFlow(emptySet())
    override suspend fun setProtection(on: Boolean) {}
    override suspend fun toggleProtection() {}
    override suspend fun reveal(id: String) {}
    override suspend fun revealAll(ids: Set<String>) {}
    override suspend fun restoreRevealed(ids: Set<String>) {}
    override fun observeReliabilityLevel(): Flow<Int> = level
    override fun observePinnedFeed(): Flow<String?> = pinned
    override suspend fun setReliabilityLevel(level: Int) { this.level.value = level }
    override suspend fun setPinnedFeed(key: String?) { pinned.value = key }
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

class FilterPrefsUseCasesTest {
    @Test fun `ObserveFilterPrefs maps stored ints and keys to domain types`() = runTest {
        val prefs = FakePrefs()
        val sut = ObserveFilterPrefsUseCase(prefs)
        assertThat(sut().first()).isEqualTo(FilterPrefs(Tier.TIER_3_SPECULATIVE, null))
        prefs.level.value = 1
        prefs.pinned.value = "match"
        assertThat(sut().first()).isEqualTo(FilterPrefs(Tier.TIER_1_RELIABLE, FeedType.MATCH))
    }

    @Test fun `ObserveFilterPrefs clamps out-of-range stored levels`() = runTest {
        val prefs = FakePrefs()
        val sut = ObserveFilterPrefsUseCase(prefs)
        prefs.level.value = 0
        assertThat(sut().first().maxTier).isEqualTo(Tier.TIER_1_RELIABLE)
        prefs.level.value = 99
        assertThat(sut().first().maxTier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `SetReliabilityLevel stores the tier as a 1-based level`() = runTest {
        val prefs = FakePrefs()
        SetReliabilityLevelUseCase(prefs)(Tier.TIER_1_RELIABLE)
        assertThat(prefs.level.value).isEqualTo(1)
        SetReliabilityLevelUseCase(prefs)(Tier.TIER_3_SPECULATIVE)
        assertThat(prefs.level.value).isEqualTo(3)
    }

    @Test fun `SetPinnedFeed stores the key and clears on null`() = runTest {
        val prefs = FakePrefs()
        SetPinnedFeedUseCase(prefs)(FeedType.ACADEMY)
        assertThat(prefs.pinned.value).isEqualTo("academy")
        SetPinnedFeedUseCase(prefs)(null)
        assertThat(prefs.pinned.value).isNull()
    }
}
