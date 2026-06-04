package dev.crushingdigits.littlebird.data.news.classifier

/**
 * Keeps only Liverpool-relevant items. Applied at ingestion to feeds flagged
 * `clubScoped = false` (e.g. a transfer-wide journalist column), so non-Liverpool
 * stories never reach the cache. Terms are lowercase; matching runs on normalized text.
 */
class LiverpoolRelevanceFilter(private val terms: Set<String> = defaultLiverpoolTerms) {
    fun isRelevant(title: String, description: String): Boolean {
        val haystack = TextNormalizer.normalize("$title $description")
        return terms.any { haystack.contains(it) }
    }
}

val defaultLiverpoolTerms: Set<String> = setOf(
    "liverpool",
    "anfield",
    "lfc",
    "the reds",
    "the kop",
)
