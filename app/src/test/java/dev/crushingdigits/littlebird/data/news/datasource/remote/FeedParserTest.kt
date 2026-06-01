package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedParserTest {
    private val subject = FeedParser()

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("feeds/$name")!!.bufferedReader().use { it.readText() }

    @Test fun `parses RSS 2_0 items with cdata, creator and media image`() {
        val items = subject.parse(fixture("rss_sample.xml"), sourceId = "tia")

        assertThat(items).hasSize(2)
        val first = items[0]
        assertThat(first.title).isEqualTo("Liverpool agree deal for midfielder")
        assertThat(first.link).isEqualTo("https://example.com/a")
        assertThat(first.guid).isEqualTo("https://example.com/a")
        assertThat(first.author).isEqualTo("David Ornstein")
        assertThat(first.description).contains("Here we go")
        assertThat(first.imageUrl).isEqualTo("https://example.com/a.jpg")
        assertThat(first.pubDate).isNotNull()
    }

    @Test fun `parses Atom entries`() {
        val items = subject.parse(fixture("atom_sample.xml"), sourceId = "bbc")
        assertThat(items).hasSize(1)
        assertThat(items[0].title).isEqualTo("New set-piece coach lined up")
        assertThat(items[0].link).isEqualTo("https://example.com/c")
        assertThat(items[0].author).isEqualTo("BBC Sport")
    }
}
