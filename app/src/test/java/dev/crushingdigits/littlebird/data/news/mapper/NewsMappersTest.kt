package dev.crushingdigits.littlebird.data.news.mapper

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier
import java.time.Instant
import org.junit.Test

class NewsMappersTest {

    private fun anyClassification() =
        Classification(isMatch = false, category = Category.OTHER, tier = Tier.TIER_2_BEAT, confirmed = false)

    @Test fun `parses single-digit day pubDate (Liverpool Echo) to a real instant`() {
        val raw = RawFeedItem(
            sourceId = "liverpool_echo", title = "T", link = "https://x/echo", description = "d",
            pubDate = "Wed, 3 Jun 2026 08:15:56 +0000", author = "Matt Addison",
            categories = emptyList(), imageUrl = null, guid = "https://x/echo",
        )
        val entity = raw.toEntity("Liverpool Echo", anyClassification())
        assertThat(entity.publishedAtEpochMs)
            .isEqualTo(Instant.parse("2026-06-03T08:15:56Z").toEpochMilli())
    }

    @Test fun `parses zone-name pubDate (BBC GMT) to a real instant`() {
        val raw = RawFeedItem(
            sourceId = "bbc_liverpool", title = "T", link = "https://x/bbc", description = "d",
            pubDate = "Wed, 03 Jun 2026 06:26:52 GMT", author = null,
            categories = emptyList(), imageUrl = null, guid = "https://x/bbc",
        )
        val entity = raw.toEntity("BBC Sport", anyClassification())
        assertThat(entity.publishedAtEpochMs)
            .isEqualTo(Instant.parse("2026-06-03T06:26:52Z").toEpochMilli())
    }

    @Test fun `parses ISO-8601 (Atom) pubDate`() {
        val raw = RawFeedItem(
            sourceId = "bbc", title = "T", link = "https://x/iso", description = "d",
            pubDate = "2026-06-01T07:00:00Z", author = null,
            categories = emptyList(), imageUrl = null, guid = "https://x/iso",
        )
        val entity = raw.toEntity("Atom", anyClassification())
        assertThat(entity.publishedAtEpochMs)
            .isEqualTo(Instant.parse("2026-06-01T07:00:00Z").toEpochMilli())
    }

    @Test fun `unparseable pubDate falls back to EPOCH`() {
        val raw = RawFeedItem(
            sourceId = "x", title = "T", link = "https://x/z", description = "d",
            pubDate = "not a date", author = null,
            categories = emptyList(), imageUrl = null, guid = "https://x/z",
        )
        val entity = raw.toEntity("X", anyClassification())
        assertThat(entity.publishedAtEpochMs).isEqualTo(0L)
    }

    @Test fun `raw + classification maps to entity with stable id from guid`() {
        val raw = RawFeedItem(
            sourceId = "tia", title = "Deal agreed", link = "https://x/a", description = "<p>terms</p>",
            pubDate = "Mon, 01 Jun 2026 09:00:00 +0000", author = "David Ornstein",
            categories = emptyList(), imageUrl = null, guid = "https://x/a",
        )
        val c = Classification(isMatch = false, category = Category.TRANSFER, tier = Tier.TIER_1_RELIABLE, confirmed = true)

        val entity = raw.toEntity(sourceName = "This Is Anfield", classification = c)

        assertThat(entity.id).isEqualTo("https://x/a")
        assertThat(entity.tier).isEqualTo("TIER_1_RELIABLE")
        assertThat(entity.category).isEqualTo("TRANSFER")
        assertThat(entity.confirmed).isTrue()
        assertThat(entity.summary).doesNotContain("<p>") // HTML stripped for display
        assertThat(entity.publishedAtEpochMs).isGreaterThan(0L)
    }

    @Test fun `falls back to link then synthetic id when guid is blank`() {
        val noGuid = RawFeedItem("tia", "T", "https://x/b", "d", null, null, emptyList(), null, guid = null)
        val entity = noGuid.toEntity("A", Classification(false, Category.OTHER, Tier.TIER_2_BEAT, false))
        assertThat(entity.id).isEqualTo("https://x/b") // falls back to link
    }

    @Test fun `entity maps back to domain NewsItem`() {
        val raw = RawFeedItem("tia", "T", "https://x/a", "d", null, null, emptyList(), null, null)
        val entity = raw.toEntity("Anfield Times", Classification(true, Category.MATCH, Tier.TIER_2_BEAT, false))
        val item = entity.toDomain()
        assertThat(item.category).isEqualTo(Category.MATCH)
        assertThat(item.tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(item.source).isEqualTo("Anfield Times") // sourceName -> source
        assertThat(item.id).isEqualTo(entity.id)
    }
}
