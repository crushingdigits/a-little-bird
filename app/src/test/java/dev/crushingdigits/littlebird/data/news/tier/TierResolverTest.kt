package dev.crushingdigits.littlebird.data.news.tier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class TierResolverTest {
    private val subject = TierResolver(defaultTierConfig)

    @Test fun `tier-1 journalist bumps tier up`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "David Ornstein", text = "deal agreed")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
    }

    @Test fun `known aggregator forced to tier-3`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "Daily Mirror", text = "shock swoop")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `falls back to provided default`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "This Is Anfield", text = "report")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
    }

    @Test fun `here we go from tier-1 marks confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Here we go! Deal done")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `here we go without tier-1 is not confirmed`() {
        val (_, confirmed) = subject.resolve(Tier.TIER_3_SPECULATIVE, "Daily Mirror", "here we go claim")
        assertThat(confirmed).isFalse()
    }
}
