package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.GameStatus
import dev.crushingdigits.littlebird.domain.model.MatchResult
import java.time.Instant

enum class FixtureState { WATCHED, UNWATCHED, UPCOMING }

/** A fixture as the sheet renders it. THE HARD RULE: [scoreLine]/[result] are non-null ONLY when WATCHED. */
data class FixtureUi(
    val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoff: Instant,
    val state: FixtureState,
    val scoreLine: String?,
    val result: MatchResult?,
    val isWall: Boolean,
)

/** Builds sheet rows. Score/result are only ever attached to WATCHED games — the single gate. */
fun List<Game>.toFixtureUi(watched: Set<String>, now: Instant, wall: Instant?): List<FixtureUi> = map { g ->
    val state = when {
        g.status(now) == GameStatus.UPCOMING -> FixtureState.UPCOMING
        g.id in watched -> FixtureState.WATCHED
        else -> FixtureState.UNWATCHED
    }
    val isWatched = state == FixtureState.WATCHED
    FixtureUi(
        id = g.id,
        opponent = g.opponent,
        abbreviation = g.abbreviation,
        isHome = g.isHome,
        competition = g.competition,
        kickoff = g.kickoff,
        state = state,
        scoreLine = if (isWatched) g.scoreLine() else null,
        result = if (isWatched) g.result() else null,
        isWall = state == FixtureState.UNWATCHED && wall != null && g.kickoff == wall,
    )
}

private fun Game.scoreLine(): String? {
    val h = homeScore ?: return null
    val a = awayScore ?: return null
    return "$h–$a" // en dash; home–away
}
