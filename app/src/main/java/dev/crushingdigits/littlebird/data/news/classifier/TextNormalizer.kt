package dev.crushingdigits.littlebird.data.news.classifier

object TextNormalizer {
    private val htmlTag = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String =
        htmlTag.replace(raw, " ")
            .replace(whitespace, " ")
            .trim()
            .lowercase()
}
