package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.OkHttpFeedFetcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class TheSportsDbScheduleDataSourceTest {
    private val server = MockWebServer()
    private lateinit var subject: TheSportsDbScheduleDataSource

    @Before fun setUp() {
        server.start()
        val config = TheSportsDbConfig(baseUrl = server.url("").toString().trimEnd('/'), apiKey = "123", teamId = "133602")
        subject = TheSportsDbScheduleDataSource(
            fetcher = OkHttpFeedFetcher(OkHttpClient()),
            json = Json { ignoreUnknownKeys = true },
            config = config,
        )
    }
    @After fun tearDown() = server.shutdown()

    @Test fun `parses last and next events and combines them`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"results":[{"idEvent":"1","strHomeTeam":"Liverpool","strAwayTeam":"Chelsea","intHomeScore":"2","intAwayScore":"1","strLeague":"English Premier League","strTimestamp":"2026-05-31T16:30:00+00:00"}]}""",
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"events":[{"idEvent":"2","strHomeTeam":"Arsenal","strAwayTeam":"Liverpool","strLeague":"English Premier League","strTimestamp":"2026-06-14T17:30:00+00:00"}]}""",
            ),
        )

        val fixtures = subject.fetchFixtures()

        assertThat(fixtures.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(fixtures[0].homeScore).isEqualTo("2")
    }

    @Test fun `tolerates malformed json on one endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"events":[{"idEvent":"2","strHomeTeam":"Arsenal","strAwayTeam":"Liverpool","strTimestamp":"2026-06-14T17:30:00+00:00"}]}""",
            ),
        )

        val fixtures = subject.fetchFixtures()

        assertThat(fixtures.map { it.id }).containsExactly("2")
    }

    @Test fun `throws when an endpoint returns an error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"events":[]}"""))

        val result = runCatching { subject.fetchFixtures() }

        assertThat(result.isFailure).isTrue()
    }
}
