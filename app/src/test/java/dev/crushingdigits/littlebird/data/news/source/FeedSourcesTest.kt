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
}
