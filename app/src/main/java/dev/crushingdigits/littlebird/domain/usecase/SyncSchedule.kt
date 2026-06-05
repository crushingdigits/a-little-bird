package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Refreshes fixtures, then (only on the very first successful sync) seeds the day-1 watched set and
 * stamps the first-sync time. Always stamps the last-sync time on success. Returns the refresh result
 * so the caller can surface offline state.
 */
fun interface SyncSchedule { suspend operator fun invoke(): Result<Unit> }

class SyncScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val prefs: PreferencesRepository,
    private val seedWatchedOnFirstSync: SeedWatchedOnFirstSync,
    private val clock: Clock,
) : SyncSchedule {
    override suspend fun invoke(): Result<Unit> {
        val result = scheduleRepository.refresh()
        if (result.isFailure) return result

        val now = clock.instant()
        if (prefs.getFirstScheduleSyncAt() == null) {
            val games = scheduleRepository.observeSchedule().first()
            prefs.setWatchedGames(seedWatchedOnFirstSync(games, now))
            prefs.setFirstScheduleSyncAt(now.toEpochMilli())
        }
        prefs.setLastScheduleSyncAt(now.toEpochMilli())
        return result
    }
}
