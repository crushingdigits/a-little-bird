package dev.crushingdigits.littlebird.data.schedule.datasource.remote

data class TheSportsDbConfig(
    val baseUrl: String = "https://www.thesportsdb.com",
    val apiKey: String = "123",
    val teamId: String = "133602", // Liverpool FC
) {
    fun lastEventsUrl() = "$baseUrl/api/v1/json/$apiKey/eventslast.php?id=$teamId"
    fun nextEventsUrl() = "$baseUrl/api/v1/json/$apiKey/eventsnext.php?id=$teamId"
}
