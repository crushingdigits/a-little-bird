package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class OkHttpFeedFetcherTest {
    private val server = MockWebServer()
    private lateinit var subject: OkHttpFeedFetcher

    @Before fun setUp() { server.start(); subject = OkHttpFeedFetcher(OkHttpClient()) }
    @After fun tearDown() { server.shutdown() }

    @Test fun `returns body on 200`() = runTest {
        server.enqueue(MockResponse().setBody("<rss></rss>").setResponseCode(200))
        val result = subject.fetch(server.url("/feed").toString())
        assertThat(result.getOrNull()).isEqualTo("<rss></rss>")
    }

    @Test fun `returns failure on error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = subject.fetch(server.url("/feed").toString())
        assertThat(result.isFailure).isTrue()
    }

    @Test fun `returns failure when host is unreachable`() = runTest {
        // Point at a port nothing is listening on (server not started for this URL).
        val result = subject.fetch("http://localhost:1/never")
        assertThat(result.isFailure).isTrue()
    }
}
