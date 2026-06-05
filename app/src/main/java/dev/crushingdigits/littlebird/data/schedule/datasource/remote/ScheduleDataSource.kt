package dev.crushingdigits.littlebird.data.schedule.datasource.remote

/** Fetches Liverpool fixtures (recent + upcoming). Throws on hard network failure. */
interface ScheduleDataSource {
    suspend fun fetchFixtures(): List<GameDto>
}
