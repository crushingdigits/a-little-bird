package dev.crushingdigits.littlebird.data.news.classifier

object TextNormalizer {
    // HTML entities (&amp; etc.) are intentionally not decoded — the FeedParser handles
    // CDATA, so titles/descriptions arrive as plain text by the time they reach here.
    private val htmlTag = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String =
        htmlTag.replace(raw, " ")
            .replace(whitespace, " ")
            .trim()
            .lowercase()
}
