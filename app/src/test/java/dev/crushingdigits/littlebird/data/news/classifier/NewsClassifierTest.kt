package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class NewsClassifierTest {
    private val subject = NewsClassifier(
        matchClassifier = MatchClassifier(defaultClassifierConfig),
        categoriser = Categoriser(defaultClassifierConfig),
        tierResolver = TierResolver(defaultTierConfig),
    )

    @Test fun `confirmed transfer from tier-1 journalist`() {
        val c = subject.classify(
            title = "Liverpool agree deal for midfielder",
            description = "Here we go! Personal terms agreed.",
            author = "Fabrizio Romano",
            defaultTier = Tier.TIER_2_BEAT,
        )
        assertThat(c.isMatch).isFalse()
        assertThat(c.category).isEqualTo(Category.TRANSFER)
        assertThat(c.tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(c.confirmed).isTrue()
    }

    @Test fun `match content is flagged and categorised MATCH`() {
        val c = subject.classify(
            title = "Player ratings: 2-1 win",
            description = "Full-time report.",
            author = "BBC Sport",
            defaultTier = Tier.TIER_2_BEAT,
        )
        assertThat(c.isMatch).isTrue()
        assertThat(c.category).isEqualTo(Category.MATCH)
        assertThat(c.confirmed).isFalse()
    }
}
