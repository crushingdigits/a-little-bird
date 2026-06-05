package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SyncScheduleTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    private class FakeRepo(private val games: List<Game>, private val ok: Boolean) : ScheduleRepository {
        val schedule = MutableStateFlow(emptyList<Game>())
        var refreshed = false
        override fun observeSchedule(): Flow<List<Game>> = schedule
        override suspend fun refresh(): Result<Unit> {
            refreshed = true
            return if (ok) { schedule.value = games; Result.success(Unit) } else Result.failure(RuntimeException())
        }
    }

    // Minimal in-memory PreferencesRepository (only the methods SyncSchedule touches).
    private class FakePrefs : dev.crushingdigits.littlebird.data.preferences.PreferencesRepository {
        var watched: Set<String> = emptySet()
        var firstSync: Long? = null
        var lastSync: Long? = null
        override fun observeProtection() = MutableStateFlow(true)
        override fun observeRevealed() = MutableStateFlow<Set<String>>(emptySet())
        override suspend fun setProtection(on: Boolean) {}
        override suspend fun toggleProtection() {}
        override suspend fun reveal(id: String) {}
        override suspend fun revealAll(ids: Set<String>) {}
        override suspend fun restoreRevealed(ids: Set<String>) {}
        override fun observeReliabilityLevel() = MutableStateFlow(3)
        override fun observePinnedFeed() = MutableStateFlow<String?>(null)
        override suspend fun setReliabilityLevel(level: Int) {}
        override suspend fun setPinnedFeed(key: String?) {}
        override fun observeWatchedGames() = MutableStateFlow(watched)
        override suspend fun markGameWatched(id: String) { watched = watched + id }
        override suspend fun unmarkGameWatched(id: String) { watched = watched - id }
        override suspend fun setWatchedGames(ids: Set<String>) { watched = ids }
        override suspend fun getFirstScheduleSyncAt() = firstSync
        override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
        override fun observeLastScheduleSyncAt() = MutableStateFlow(lastSync)
        override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync = epochMs }
    }

    private fun subject(repo: ScheduleRepository, prefs: FakePrefs) =
        SyncScheduleUseCase(repo, prefs, SeedWatchedOnFirstSyncUseCase(), clock)

    @Test fun `first successful sync seeds old games as watched and stamps timestamps`() = runTest {
        val games = listOf(game("old", "2026-05-20T16:00:00Z"), game("recent", "2026-06-04T16:00:00Z"))
        val prefs = FakePrefs()
        val result = subject(FakeRepo(games, ok = true), prefs).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(prefs.watched).containsExactly("old")
        assertThat(prefs.firstSync).isEqualTo(now.toEpochMilli())
        assertThat(prefs.lastSync).isEqualTo(now.toEpochMilli())
    }

    @Test fun `a later sync does not re-seed but updates lastSync`() = runTest {
        val prefs = FakePrefs().apply { firstSync = 999L; watched = setOf("manual") }
        val result = subject(FakeRepo(listOf(game("old", "2026-05-20T16:00:00Z")), ok = true), prefs).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(prefs.watched).containsExactly("manual") // unchanged
        assertThat(prefs.firstSync).isEqualTo(999L)         // unchanged
        assertThat(prefs.lastSync).isEqualTo(now.toEpochMilli())
    }

    @Test fun `a failed sync seeds nothing and does not stamp timestamps`() = runTest {
        val prefs = FakePrefs()
        val result = subject(FakeRepo(emptyList(), ok = false), prefs).invoke()

        assertThat(result.isFailure).isTrue()
        assertThat(prefs.watched).isEmpty()
        assertThat(prefs.firstSync).isNull()
        assertThat(prefs.lastSync).isNull()
    }
}
