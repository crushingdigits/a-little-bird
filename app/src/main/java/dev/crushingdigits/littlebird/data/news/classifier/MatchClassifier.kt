package dev.crushingdigits.littlebird.data.news.classifier

class MatchClassifier(private val config: ClassifierConfig) {

    private val signalRegexes: List<Regex> = config.matchSignals.map { signal ->
        val escaped = Regex.escape(signal)
        val prefix = if (signal.first().isLetterOrDigit()) "\\b" else ""
        val suffix = if (signal.last().isLetterOrDigit()) "\\b" else ""
        Regex(prefix + escaped + suffix)
    }

    private val allowlistRegexes: List<Regex> = config.transferContextAllowlist.map { Regex(Regex.escape(it)) }

    /** @param text raw title + " " + description; normalized internally. */
    fun isMatch(text: String): Boolean {
        val t = TextNormalizer.normalize(text)
        if (config.scorelineRegex.containsMatchIn(t)) return true
        val hit = signalRegexes.any { it.containsMatchIn(t) }
        if (!hit) return false
        return allowlistRegexes.none { it.containsMatchIn(t) }
    }
}
