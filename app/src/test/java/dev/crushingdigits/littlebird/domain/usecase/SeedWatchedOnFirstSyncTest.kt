package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Game
import org.junit.Test
import java.time.Instant

class SeedWatchedOnFirstSyncTest {
    private val firstSync = Instant.parse("2026-06-05T12:00:00Z")
    private val subject = SeedWatchedOnFirstSyncUseCase()

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    @Test fun `seeds games older than the window as watched, leaving recent and future unwatched`() {
        val games = listOf(
            game("old", "2026-05-20T16:00:00Z"),    // > 3 days before -> seeded
            game("recent", "2026-06-04T16:00:00Z"), // within 3 days -> NOT seeded
            game("future", "2026-06-10T16:00:00Z"), // future -> NOT seeded
        )
        val seeded = subject(games, firstSync, windowDays = 3)
        assertThat(seeded).containsExactly("old")
    }

    @Test fun `the window boundary is exclusive of the cutoff`() {
        // cutoff = firstSync - 3 days = 2026-06-02T12:00:00Z; a game exactly at cutoff is NOT seeded
        val games = listOf(game("boundary", "2026-06-02T12:00:00Z"))
        assertThat(subject(games, firstSync, windowDays = 3)).isEmpty()
    }
}
