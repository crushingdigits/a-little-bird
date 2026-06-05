package dev.crushingdigits.littlebird.ui.feed.wall

/** Maps a full competition/league name to a short tag for the fixtures sheet. */
fun competitionTag(competition: String): String = when {
    competition.contains("Premier League", ignoreCase = true) -> "PL"
    competition.contains("Champions League", ignoreCase = true) -> "UCL"
    competition.contains("Europa League", ignoreCase = true) -> "UEL"
    competition.contains("FA Cup", ignoreCase = true) -> "FA"
    competition.contains("Carabao", ignoreCase = true) ||
        competition.contains("EFL Cup", ignoreCase = true) ||
        competition.contains("League Cup", ignoreCase = true) -> "EFL"
    competition.isBlank() -> "—"
    else -> competition.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(3)
}
