package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.Game
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Day-1 seed: games whose kickoff is strictly before (firstSyncAt - window) are pre-marked watched. */
interface SeedWatchedOnFirstSync {
    operator fun invoke(games: List<Game>, firstSyncAt: Instant, windowDays: Long = 3): Set<String>
}

class SeedWatchedOnFirstSyncUseCase @Inject constructor() : SeedWatchedOnFirstSync {
    override fun invoke(games: List<Game>, firstSyncAt: Instant, windowDays: Long): Set<String> {
        val cutoff = firstSyncAt.minus(Duration.ofDays(windowDays))
        return games.filter { it.kickoff.isBefore(cutoff) }.map { it.id }.toSet()
    }
}
