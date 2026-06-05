package dev.crushingdigits.littlebird.data.schedule.mapper

import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameEntity
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.GameDto
import dev.crushingdigits.littlebird.domain.model.Game
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val LIVERPOOL = "Liverpool"

/** Prefer the ISO timestamp; fall back to dateEvent + strTime (assumed UTC). Null if neither parses. */
private fun parseKickoff(dto: GameDto): Instant? {
    dto.timestamp?.takeIf { it.isNotBlank() }?.let { ts ->
        runCatching { OffsetDateTime.parse(ts).toInstant() }.getOrNull()?.let { return it }
    }
    val date = dto.date?.takeIf { it.isNotBlank() } ?: return null
    val time = dto.time?.takeIf { it.isNotBlank() } ?: "00:00:00"
    return runCatching {
        LocalDate.parse(date).atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC)
    }.getOrNull()
}

/** Maps a TheSportsDB event to a row, from Liverpool's perspective. Null if the kickoff is unparseable. */
fun GameDto.toEntity(): GameEntity? {
    val kickoff = parseKickoff(this) ?: return null
    val home = homeTeam.equals(LIVERPOOL, ignoreCase = true)
    val opponent = if (home) awayTeam else homeTeam
    return GameEntity(
        id = id,
        opponent = opponent,
        abbreviation = opponent.filter { it.isLetter() }.take(3).uppercase(),
        isHome = home,
        competition = league.orEmpty(),
        kickoffEpochMs = kickoff.toEpochMilli(),
        homeScore = homeScore?.toIntOrNull(),
        awayScore = awayScore?.toIntOrNull(),
    )
}

fun GameEntity.toDomain(): Game = Game(
    id = id,
    opponent = opponent,
    abbreviation = abbreviation,
    isHome = isHome,
    competition = competition,
    kickoff = Instant.ofEpochMilli(kickoffEpochMs),
    homeScore = homeScore,
    awayScore = awayScore,
)
