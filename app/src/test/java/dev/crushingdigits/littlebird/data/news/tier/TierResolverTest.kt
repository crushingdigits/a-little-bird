package dev.crushingdigits.littlebird.data.news.tier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class TierResolverTest {
    private val subject = TierResolver(defaultTierConfig)

    @Test fun `trusted journalist with confirmation language is tier-1 and confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Ornstein", "deal agreed for midfielder")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `here we go from a trusted journalist marks confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Here we go! Deal done")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `david lynch is recognised as a trusted journalist`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Lynch", "Here we go, medical completed")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `trusted journalist with tentative language caps at tier-2`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Liverpool interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(confirmed).isFalse()
    }

    @Test fun `neutral journalist item falls back to the source floor`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Lynch", "why Liverpool sacked the head coach")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(confirmed).isFalse()
    }

    @Test fun `tabloid author is forced to tier-3`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "Daily Mirror", "shock swoop")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `tabloid name-dropping a journalist tentatively stays tier-3`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "The Sun", "Fabrizio Romano says Liverpool interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `tabloid is rescued to tier-1 only by a journalist confirmation`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "The Sun", "Fabrizio Romano: here we go, done deal")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `inherently reliable source is not downgraded by a tentative journalist quote`() {
        // Full name so the journalist IS detected; tentative language would cap a T2 source at T2,
        // but the T1 institutional floor must hold (exercises moreReliable).
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "David Ornstein: Liverpool are interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isFalse()
    }

    @Test fun `institutional confirmation without a journalist still lights confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "Liverpool complete the signing — done deal")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `non-transfer use of the word confirmed does not light confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "Liverpool's win confirmed at Anfield")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isFalse()
    }

    @Test fun `unknown source falls back to the provided default`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "This Is Anfield", "match report")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
    }
}
