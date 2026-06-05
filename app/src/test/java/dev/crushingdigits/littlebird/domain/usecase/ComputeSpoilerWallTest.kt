package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Game
import org.junit.Test
import java.time.Instant

class ComputeSpoilerWallTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val subject = ComputeSpoilerWallUseCase()

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    @Test fun `wall is the earliest played unwatched kickoff`() {
        val games = listOf(
            game("a", "2026-06-01T16:00:00Z"), // played, unwatched -> wall
            game("b", "2026-06-03T16:00:00Z"), // played, unwatched
            game("c", "2026-06-10T16:00:00Z"), // upcoming
        )
        val wall = subject(games, watched = emptySet(), now = now)
        assertThat(wall).isEqualTo(Instant.parse("2026-06-01T16:00:00Z"))
    }

    @Test fun `watched games do not count and the wall advances`() {
        val games = listOf(game("a", "2026-06-01T16:00:00Z"), game("b", "2026-06-03T16:00:00Z"))
        val wall = subject(games, watched = setOf("a"), now = now)
        assertThat(wall).isEqualTo(Instant.parse("2026-06-03T16:00:00Z"))
    }

    @Test fun `upcoming games never form a wall`() {
        val games = listOf(game("c", "2026-06-10T16:00:00Z"))
        assertThat(subject(games, watched = emptySet(), now = now)).isNull()
    }

    @Test fun `all caught up gives a null wall`() {
        val games = listOf(game("a", "2026-06-01T16:00:00Z"))
        assertThat(subject(games, watched = setOf("a"), now = now)).isNull()
    }
}
