package dev.crushingdigits.littlebird.domain.model

import java.time.Instant

enum class GameStatus { PLAYED, UPCOMING }

enum class MatchResult { WIN, DRAW, LOSS }

/** A Liverpool fixture. Scores are present once the game has been played; `null` otherwise. */
data class Game(
    val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoff: Instant,
    val homeScore: Int?,
    val awayScore: Int?,
) {
    fun status(now: Instant): GameStatus =
        if (!kickoff.isAfter(now)) GameStatus.PLAYED else GameStatus.UPCOMING

    /** Liverpool's goals / opponent's goals from Liverpool's perspective, or null if not played. */
    val liverpoolScore: Int? get() = if (isHome) homeScore else awayScore
    val opponentScore: Int? get() = if (isHome) awayScore else homeScore

    /** Win/Draw/Loss from Liverpool's perspective. Null when the score is unknown. */
    fun result(): MatchResult? {
        val us = liverpoolScore ?: return null
        val them = opponentScore ?: return null
        return when {
            us > them -> MatchResult.WIN
            us < them -> MatchResult.LOSS
            else -> MatchResult.DRAW
        }
    }
}
