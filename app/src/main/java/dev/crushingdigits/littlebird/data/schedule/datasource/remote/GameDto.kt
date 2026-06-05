package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One fixture/event as returned by TheSportsDB. Scores are strings ("2") or null until played. */
@Serializable
data class GameDto(
    @SerialName("idEvent") val id: String,
    @SerialName("strHomeTeam") val homeTeam: String = "",
    @SerialName("strAwayTeam") val awayTeam: String = "",
    @SerialName("intHomeScore") val homeScore: String? = null,
    @SerialName("intAwayScore") val awayScore: String? = null,
    @SerialName("strLeague") val league: String? = null,
    @SerialName("strTimestamp") val timestamp: String? = null,
    @SerialName("dateEvent") val date: String? = null,
    @SerialName("strTime") val time: String? = null,
)

/** `eventslast.php` wraps fixtures under "results"; `eventsnext.php` under "events". */
@Serializable
data class EventsLastResponse(@SerialName("results") val results: List<GameDto>? = null)

@Serializable
data class EventsNextResponse(@SerialName("events") val events: List<GameDto>? = null)
