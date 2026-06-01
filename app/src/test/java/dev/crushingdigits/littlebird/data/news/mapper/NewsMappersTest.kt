package dev.crushingdigits.littlebird.data.news.mapper

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class NewsMappersTest {

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
