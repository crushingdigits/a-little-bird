package dev.crushingdigits.littlebird.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedTypeTest {
    @Test fun `ALL matches every category`() {
        Category.entries.forEach { assertThat(FeedType.ALL.matches(it)).isTrue() }
    }

    @Test fun `SQUAD_STAFF matches coaching and backroom only`() {
        assertThat(FeedType.SQUAD_STAFF.matches(Category.COACHING)).isTrue()
        assertThat(FeedType.SQUAD_STAFF.matches(Category.BACKROOM)).isTrue()
        assertThat(FeedType.SQUAD_STAFF.matches(Category.TRANSFER)).isFalse()
    }

    @Test fun `fromKey resolves known keys and falls back to ALL`() {
        assertThat(FeedType.fromKey("transfer")).isEqualTo(FeedType.TRANSFERS)
        assertThat(FeedType.fromKey("academy")).isEqualTo(FeedType.ACADEMY)
        assertThat(FeedType.fromKey(null)).isEqualTo(FeedType.ALL)
        assertThat(FeedType.fromKey("nope")).isEqualTo(FeedType.ALL)
    }
}
