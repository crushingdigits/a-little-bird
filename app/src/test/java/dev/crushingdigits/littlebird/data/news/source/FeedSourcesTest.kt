package dev.crushingdigits.littlebird.data.news.source

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class FeedSourcesTest {
    private fun source(id: String) = FeedSources.all.single { it.id == id }

    @Test fun `bbc is an inherently reliable tier-1 source`() {
        assertThat(source("bbc_liverpool").defaultTier).isEqualTo(Tier.TIER_1_RELIABLE)
    }

    @Test fun `feed sources are club-scoped by default`() {
        assertThat(source("bbc_liverpool").clubScoped).isTrue()
        assertThat(source("this_is_anfield").clubScoped).isTrue()
        assertThat(source("liverpool_echo").clubScoped).isTrue()
    }

    @Test fun `guardian is an inherently reliable club-scoped tier-1 source`() {
        val guardian = source("guardian_liverpool")
        assertThat(guardian.defaultTier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(guardian.clubScoped).isTrue()
    }

    @Test fun `david lynch is a club-scoped tier-2 beat source`() {
        val lynch = source("david_lynch")
        assertThat(lynch.defaultTier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(lynch.clubScoped).isTrue()
    }
}
