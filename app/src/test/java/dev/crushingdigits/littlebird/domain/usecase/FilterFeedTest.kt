package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import java.time.Instant

class FilterFeedTest {
    private val subject = FilterFeedUseCase()

    private fun item(id: String, tier: Tier, category: Category) = NewsItem(
        id = id, source = "s", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private val feed = listOf(
        item("t1", Tier.TIER_1_RELIABLE, Category.TRANSFER),
        item("m2", Tier.TIER_2_BEAT, Category.MATCH),
        item("a3", Tier.TIER_3_SPECULATIVE, Category.ACADEMY),
        item("c1", Tier.TIER_1_RELIABLE, Category.COACHING),
    )

    @Test fun `tier ceiling is cumulative`() {
        val r = subject(feed, FeedFilter(FeedType.ALL, Tier.TIER_2_BEAT))
        assertThat(r.visible.map { it.id }).containsExactly("t1", "m2", "c1") // T3 academy excluded
    }

    @Test fun `type filter selects only matching categories`() {
        val r = subject(feed, FeedFilter(FeedType.SQUAD_STAFF, Tier.TIER_3_SPECULATIVE))
        assertThat(r.visible.map { it.id }).containsExactly("c1")
    }

    @Test fun `counts are per type at the current tier ceiling`() {
        val r = subject(feed, FeedFilter(FeedType.ALL, Tier.TIER_1_RELIABLE))
        assertThat(r.counts[FeedType.ALL]).isEqualTo(2)       // t1 + c1
        assertThat(r.counts[FeedType.TRANSFERS]).isEqualTo(1) // t1
        assertThat(r.counts[FeedType.MATCH]).isEqualTo(0)     // m2 is T2, excluded
        assertThat(r.counts[FeedType.ACADEMY]).isEqualTo(0)   // a3 is T3, excluded
    }

    @Test fun `empty when no item matches`() {
        val r = subject(feed, FeedFilter(FeedType.MATCH, Tier.TIER_1_RELIABLE))
        assertThat(r.visible).isEmpty()
    }
}
