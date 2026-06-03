package dev.crushingdigits.littlebird.ui.util

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import java.time.Instant

class RelativeTimeTest {
    private fun at(epochMs: Long) = NewsItem(
        id = "x", source = "s", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "l", imageUrl = null, publishedAt = Instant.ofEpochMilli(epochMs),
    )
    private val now = Instant.ofEpochSecond(10_000_000)

    @Test fun `formats minutes hours days and now`() {
        assertThat(at(now.toEpochMilli()).relativeTime(now)).isEqualTo("now")
        assertThat(at(now.minusSeconds(120).toEpochMilli()).relativeTime(now)).isEqualTo("2m")
        assertThat(at(now.minusSeconds(7200).toEpochMilli()).relativeTime(now)).isEqualTo("2h")
        assertThat(at(now.minusSeconds(172800).toEpochMilli()).relativeTime(now)).isEqualTo("2d")
    }
}
