package dev.crushingdigits.littlebird.data.news.mapper

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

/**
 * Converts an RSS/Atom feed date string into epoch milliseconds (UTC), or `0L` when it cannot be
 * understood. Built on kotlinx-datetime rather than `DateTimeFormatter.RFC_1123_DATE_TIME` to close
 * two failure modes of the JDK formatter that would silently sink a feed to the 1970 epoch:
 *
 * 1. **Weekday validation.** RFC_1123_DATE_TIME validates the leading weekday against the date, so a
 *    feed with a wrong or missing weekday (`Mon, 3 Jun 2026 …` where 3 Jun is a Wednesday) is
 *    rejected. Here the weekday token is stripped up front and **never** validated.
 * 2. **Named zones.** RFC_1123_DATE_TIME understands only `GMT` and numeric offsets, not `BST`,
 *    `EST`, etc. Here a fixed, documented set of abbreviations is rewritten to its numeric offset.
 *
 * ## Supported input — and nothing else
 * - **RFC-822/1123** (RSS `pubDate`): optional weekday, single- or double-digit day, English
 *   abbreviated month (`Jun`), 4-digit year, `HH:mm` with optional `:ss`, then a 4-digit numeric
 *   offset (`+0000`) or one of the recognised zone abbreviations below.
 * - **ISO-8601** (Atom): `2026-06-01T07:00:00Z` and `2026-06-01T07:00:00+02:00`.
 *
 * ## Recognised zone abbreviations (the ceiling — anything else falls through to `0L`)
 * `GMT`/`UTC`/`UT`/`Z` → `+0000`, `BST`/`CET` → `+0100`, `CEST` → `+0200`,
 * `EST` → `-0500`, `EDT` → `-0400`, `CST` → `-0600`, `CDT` → `-0500`, `MST` → `-0700`,
 * `MDT` → `-0600`, `PST` → `-0800`, `PDT` → `-0700`. An abbreviation that is not in this map is
 * left untouched, so parsing fails and the result is `0L` — there is no silent guessing.
 * `null`/blank input is also `0L`.
 */
internal fun parseFeedDate(raw: String?): Long {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return 0L
    val normalized = normalize(trimmed)
    return rfc822Format.parseOrNull(normalized)?.toInstantUsingOffset()?.toEpochMilliseconds()
        ?: DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parseOrNull(trimmed)
            ?.toInstantUsingOffset()?.toEpochMilliseconds()
        ?: 0L
}

/** Strip the leading weekday token, collapse internal whitespace, then rewrite a trailing recognised zone abbreviation. */
private fun normalize(value: String): String {
    val withoutWeekday = leadingWeekday.replaceFirst(value, "")
    val collapsed = withoutWeekday.replace(Regex("\\s+"), " ").trim()
    return trailingZone.replace(collapsed) { match ->
        zoneOffsets[match.value.uppercase()] ?: match.value
    }
}

private val leadingWeekday = Regex("^[A-Za-z]{3,},?\\s*")
private val trailingZone = Regex("\\b([A-Za-z]{1,4})$")

/** Explicit, documented zone abbreviation → 4-digit numeric offset. No silent magic. */
private val zoneOffsets: Map<String, String> = mapOf(
    "GMT" to "+0000",
    "UTC" to "+0000",
    "UT" to "+0000",
    "Z" to "+0000",
    "BST" to "+0100",
    "CET" to "+0100",
    "CEST" to "+0200",
    "EST" to "-0500",
    "EDT" to "-0400",
    "CST" to "-0600",
    "CDT" to "-0500",
    "MST" to "-0700",
    "MDT" to "-0600",
    "PST" to "-0800",
    "PDT" to "-0700",
)

/**
 * RFC-822/1123 body, weekday already stripped. Day is unpadded so both `3 Jun` and `03 Jun` parse;
 * seconds are optional; the zone is always a 4-digit numeric offset post-normalization.
 */
private val rfc822Format = DateTimeComponents.Format {
    day(Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    optional {
        char(':')
        second()
    }
    char(' ')
    offset(UtcOffset.Formats.FOUR_DIGITS)
}
