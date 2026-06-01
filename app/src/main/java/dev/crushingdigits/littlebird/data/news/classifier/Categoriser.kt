package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.domain.model.Category

class Categoriser(private val config: ClassifierConfig) {

    private val regexesByCategory: List<Pair<Category, List<Regex>>> =
        config.categoryKeywords.map { (cat, words) ->
            cat to words.map { Regex("\\b" + Regex.escape(it)) } // prefix-boundary; allows sign/signing/signed
        }

    fun categorise(text: String, isMatch: Boolean): Category {
        if (isMatch) return Category.MATCH
        val t = TextNormalizer.normalize(text)
        for ((cat, regexes) in regexesByCategory) {
            if (regexes.any { it.containsMatchIn(t) }) return cat
        }
        return Category.OTHER
    }
}
