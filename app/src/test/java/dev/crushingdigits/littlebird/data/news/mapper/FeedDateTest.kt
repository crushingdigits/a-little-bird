package dev.crushingdigits.littlebird.data.news.mapper

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import org.junit.Test

/**
 * Pure-JVM coverage for [parseFeedDate]. No Robolectric — minSdk 29 has java.time natively, but the
 * parser itself uses kotlinx-datetime and is framework-free, so plain JUnit + Truth suffice.
 *
 * Expected epochs are computed with [Instant.parse] of the canonical UTC instant so the assertion
 * encodes the wall-clock-to-UTC conversion independently of the parser under test.
 */
class FeedDateTest {

    private fun epochOf(utc: String): Long = Instant.parse(utc).toEpochMilliseconds()

    @Test fun `RFC-822 single-digit day with numeric offset`() {
        assertThat(parseFeedDate("Wed, 3 Jun 2026 08:15:56 +0000"))
            .isEqualTo(epochOf("2026-06-03T08:15:56Z"))
    }

    @Test fun `RFC-1123 two-digit day with GMT zone name`() {
        assertThat(parseFeedDate("Wed, 03 Jun 2026 06:26:52 GMT"))
            .isEqualTo(epochOf("2026-06-03T06:26:52Z"))
    }

    @Test fun `RFC-1123 with explicit zero offset`() {
        assertThat(parseFeedDate("Mon, 01 Jun 2026 09:00:00 +0000"))
            .isEqualTo(epochOf("2026-06-01T09:00:00Z"))
    }

    @Test fun `numeric positive offset is subtracted to reach UTC`() {
        assertThat(parseFeedDate("Sat, 06 Jun 2026 12:00:00 +0100"))
            .isEqualTo(epochOf("2026-06-06T11:00:00Z"))
    }

    @Test fun `missing weekday still parses`() {
        assertThat(parseFeedDate("06 Jun 2026 12:00:00 +0100"))
            .isEqualTo(epochOf("2026-06-06T11:00:00Z"))
    }

    @Test fun `BST named zone maps to plus one hour`() {
        assertThat(parseFeedDate("Mon, 1 Jun 2026 10:00:00 BST"))
            .isEqualTo(epochOf("2026-06-01T09:00:00Z"))
    }

    @Test fun `EST named zone maps to minus five hours`() {
        assertThat(parseFeedDate("Tue, 2 Jun 2026 12:00:00 EST"))
            .isEqualTo(epochOf("2026-06-02T17:00:00Z"))
    }

    @Test fun `wrong weekday is ignored and still parses`() {
        // 3 Jun 2026 is a Wednesday; the feed wrongly says Mon. RFC_1123_DATE_TIME would reject this.
        assertThat(parseFeedDate("Mon, 3 Jun 2026 08:00:00 +0000"))
            .isEqualTo(epochOf("2026-06-03T08:00:00Z"))
    }

    @Test fun `ISO-8601 Atom Z`() {
        assertThat(parseFeedDate("2026-06-01T07:00:00Z"))
            .isEqualTo(epochOf("2026-06-01T07:00:00Z"))
    }

    @Test fun `ISO-8601 Atom with offset`() {
        assertThat(parseFeedDate("2026-06-01T07:00:00+02:00"))
            .isEqualTo(epochOf("2026-06-01T05:00:00Z"))
    }

    @Test fun `unparseable string is zero`() {
        assertThat(parseFeedDate("not a date")).isEqualTo(0L)
    }

    @Test fun `empty string is zero`() {
        assertThat(parseFeedDate("")).isEqualTo(0L)
    }

    @Test fun `null is zero`() {
        assertThat(parseFeedDate(null)).isEqualTo(0L)
    }

    @Test fun `folded whitespace between tokens still parses`() {
        assertThat(parseFeedDate("Wed,  3  Jun  2026  08:15:56  +0000"))
            .isEqualTo(epochOf("2026-06-03T08:15:56Z"))
    }

    @Test fun `lowercase mixed-case named zone is uppercased before lookup`() {
        assertThat(parseFeedDate("Mon, 1 Jun 2026 10:00:00 bst"))
            .isEqualTo(epochOf("2026-06-01T09:00:00Z"))
    }

    @Test fun `seconds absent still parses`() {
        assertThat(parseFeedDate("Mon, 1 Jun 2026 10:00 +0000"))
            .isEqualTo(epochOf("2026-06-01T10:00:00Z"))
    }

    @Test fun `CET named zone maps to plus one hour`() {
        assertThat(parseFeedDate("Mon, 1 Jun 2026 10:00:00 CET"))
            .isEqualTo(epochOf("2026-06-01T09:00:00Z"))
    }

    @Test fun `CEST named zone maps to plus two hours`() {
        assertThat(parseFeedDate("Tue, 2 Jun 2026 10:00:00 CEST"))
            .isEqualTo(epochOf("2026-06-02T08:00:00Z"))
    }
}
