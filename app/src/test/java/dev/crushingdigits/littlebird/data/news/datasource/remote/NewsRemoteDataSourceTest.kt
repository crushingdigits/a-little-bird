package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewsRemoteDataSourceTest {

    private val rss = """
        <rss version="2.0"><channel>
          <item><title>Deal agreed</title><link>https://x/a</link><description>terms</description></item>
        </channel></rss>
    """.trimIndent()

    private class FakeFetcher(private val responses: Map<String, Result<String>>) : FeedFetcher {
        override suspend fun fetch(url: String): Result<String> =
            responses[url] ?: Result.failure(IllegalStateException("no stub for $url"))
    }

    private val sourceA = FeedSource("a", "A", "https://x/a-feed", Tier.TIER_2_BEAT)
    private val sourceB = FeedSource("b", "B", "https://x/b-feed", Tier.TIER_2_BEAT)

    @Test fun `aggregates parsed items across sources, skipping failed feeds`() = runTest {
        val fetcher = FakeFetcher(
            mapOf(
                sourceA.url to Result.success(rss),
                sourceB.url to Result.failure(RuntimeException("down")),
            ),
        )
        val subject = NewsRemoteDataSource(fetcher, FeedParser())

        val items = subject.fetchAll(listOf(sourceA, sourceB))

        assertThat(items).hasSize(1)
        assertThat(items[0].sourceId).isEqualTo("a")
        assertThat(items[0].title).isEqualTo("Deal agreed")
    }

    @Test fun `returns empty list when all feeds fail`() = runTest {
        val fetcher = FakeFetcher(
            mapOf(
                sourceA.url to Result.failure(RuntimeException("down")),
                sourceB.url to Result.failure(RuntimeException("down")),
            ),
        )
        val subject = NewsRemoteDataSource(fetcher, FeedParser())
        assertThat(subject.fetchAll(listOf(sourceA, sourceB))).isEmpty()
    }
}
