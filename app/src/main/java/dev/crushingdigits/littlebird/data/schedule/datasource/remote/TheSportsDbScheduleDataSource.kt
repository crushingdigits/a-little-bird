package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import kotlinx.serialization.json.Json

/**
 * Fetches Liverpool fixtures from TheSportsDB (free tier: last + next events).
 * A hard network failure on either endpoint propagates (caller treats it as offline);
 * a malformed body for one endpoint is tolerated and contributes no fixtures.
 */
class TheSportsDbScheduleDataSource(
    private val fetcher: FeedFetcher,
    private val json: Json,
    private val config: TheSportsDbConfig = TheSportsDbConfig(),
) : ScheduleDataSource {

    override suspend fun fetchFixtures(): List<GameDto> {
        val lastBody = fetcher.fetch(config.lastEventsUrl()).getOrThrow()
        val nextBody = fetcher.fetch(config.nextEventsUrl()).getOrThrow()
        val last = runCatching { json.decodeFromString<EventsLastResponse>(lastBody).results.orEmpty() }
            .getOrDefault(emptyList())
        val next = runCatching { json.decodeFromString<EventsNextResponse>(nextBody).events.orEmpty() }
            .getOrDefault(emptyList())
        return last + next
    }
}
