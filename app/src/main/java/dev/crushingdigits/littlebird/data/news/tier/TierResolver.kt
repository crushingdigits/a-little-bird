package dev.crushingdigits.littlebird.data.news.tier

import dev.crushingdigits.littlebird.data.news.classifier.TextNormalizer
import dev.crushingdigits.littlebird.domain.model.Tier

class TierResolver(private val config: TierConfig) {

    /** @return resolved tier + whether the item is a confirmed ("here we go") story. */
    fun resolve(defaultTier: Tier, author: String?, text: String): Pair<Tier, Boolean> {
        val haystack = TextNormalizer.normalize((author.orEmpty()) + " " + text)
        val authorNorm = author?.let { TextNormalizer.normalize(it) }.orEmpty()

        val tier = when {
            config.tier1Journalists.any { haystack.contains(it) } -> Tier.TIER_1_RELIABLE
            config.tier3Sources.any { authorNorm.contains(it) } -> Tier.TIER_3_SPECULATIVE
            else -> defaultTier
        }
        val confirmed = tier == Tier.TIER_1_RELIABLE && haystack.contains(config.confirmedPhrase)
        return tier to confirmed
    }
}
