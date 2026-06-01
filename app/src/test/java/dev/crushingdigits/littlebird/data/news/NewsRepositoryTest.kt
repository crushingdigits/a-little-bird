package dev.crushingdigits.littlebird.data.news

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.classifier.Categoriser
import dev.crushingdigits.littlebird.data.news.classifier.MatchClassifier
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.classifier.defaultClassifierConfig
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewsRepositoryTest {

    private lateinit var db: LittleBirdDatabase
    private lateinit var local: NewsLocalDataSource

    private val classifier = NewsClassifier(
        MatchClassifier(defaultClassifierConfig),
        Categoriser(defaultClassifierConfig),
        TierResolver(defaultTierConfig),
    )
    private val source = FeedSource("tia", "This Is Anfield", "https://x/feed", Tier.TIER_2_BEAT)

    private fun rss(title: String, desc: String) = """
        <rss version="2.0"><channel>
          <item><title>$title</title><link>https://x/${title.hashCode()}</link>
            <dc:creator>David Ornstein</dc:creator><description>$desc</description></item>
        </channel></rss>
    """.trimIndent()

    private class FakeFetcher(private val byUrl: Map<String, Result<String>>) : FeedFetcher {
        override suspend fun fetch(url: String): Result<String> =
            byUrl[url] ?: Result.failure(IllegalStateException("no stub for $url"))
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LittleBirdDatabase::class.java).build()
        local = NewsLocalDataSource(db.newsDao())
    }
    @After fun tearDown() = db.close()

    private fun repo(fetcher: FeedFetcher) =
        NewsRepositoryImpl(NewsRemoteDataSource(fetcher, FeedParser()), local, classifier, listOf(source))

    @Test fun `refresh fetches, classifies, persists — observeFeed emits classified domain`() = runTest {
        val subject = repo(FakeFetcher(mapOf(source.url to Result.success(rss("Liverpool agree deal for midfielder", "Here we go")))))

        assertThat(subject.refresh().isSuccess).isTrue()

        val items = subject.observeFeed().first()
        assertThat(items).hasSize(1)
        assertThat(items[0].category).isEqualTo(Category.TRANSFER)
        assertThat(items[0].tier).isEqualTo(Tier.TIER_1_RELIABLE) // Ornstein byline bumps tier
        assertThat(items[0].source).isEqualTo("This Is Anfield")
    }

    @Test fun `network failure leaves the cached feed intact`() = runTest {
        repo(FakeFetcher(mapOf(source.url to Result.success(rss("Deal agreed", "terms"))))).refresh()
        assertThat(local.count()).isEqualTo(1)

        val failing = repo(FakeFetcher(mapOf(source.url to Result.failure(RuntimeException("offline")))))
        failing.refresh()
        assertThat(failing.observeFeed().first()).hasSize(1) // cache preserved
    }

    @Test fun `dedup by stable id across refreshes`() = runTest {
        val subject = repo(FakeFetcher(mapOf(source.url to Result.success(rss("Same headline", "x")))))
        subject.refresh()
        subject.refresh() // same link -> same stable id -> replaced, not duplicated
        assertThat(subject.observeFeed().first()).hasSize(1)
        assertThat(subject.isEmpty()).isFalse()
    }
}
