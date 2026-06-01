package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier

class NewsClassifier(
    private val matchClassifier: MatchClassifier,
    private val categoriser: Categoriser,
    private val tierResolver: TierResolver,
) {
    fun classify(title: String, description: String, author: String?, defaultTier: Tier): Classification {
        val text = "$title $description"
        val isMatch = matchClassifier.isMatch(text)
        val category = categoriser.categorise(text, isMatch)
        val (tier, confirmed) = tierResolver.resolve(defaultTier, author, text)
        return Classification(isMatch = isMatch, category = category, tier = tier, confirmed = confirmed)
    }
}
