package dev.crushingdigits.littlebird.data.news.tier

import dev.crushingdigits.littlebird.data.news.classifier.TextNormalizer
import dev.crushingdigits.littlebird.domain.model.Tier

class TierResolver(private val config: TierConfig) {

    /**
     * Resolves the single reliability tier from two factors:
     *  - the source's institutional floor ([defaultTier]), never downgraded by wording;
     *  - a trusted-journalist override gated by language (confirmation -> T1, tentative -> T2).
     *
     * @return resolved tier + whether the item is a confirmed ("done deal") story.
     */
    fun resolve(defaultTier: Tier, author: String?, text: String): Pair<Tier, Boolean> {
        val haystack = TextNormalizer.normalize(author.orEmpty() + " " + text)
        val authorNorm = author?.let { TextNormalizer.normalize(it) }.orEmpty()

        val journalist = config.tier1Journalists.any { haystack.contains(it) }
        val tabloid = config.tier3Sources.any { authorNorm.contains(it) }
        val confirming = config.confirmationPhrases.any { haystack.contains(it) }
        val tentative = config.tentativePhrases.any { haystack.contains(it) }

        val tier = when {
            journalist && confirming -> Tier.TIER_1_RELIABLE
            tabloid -> Tier.TIER_3_SPECULATIVE
            journalist && tentative -> moreReliable(defaultTier, Tier.TIER_2_BEAT)
            else -> defaultTier
        }
        val confirmed = tier == Tier.TIER_1_RELIABLE && confirming
        return tier to confirmed
    }

    // Tier ordinals: TIER_1=0 (most reliable) .. TIER_3=2 (least). The smaller ordinal wins,
    // so a tentative journalist quote never drops an inherently-reliable (T1) source below its floor.
    private fun moreReliable(a: Tier, b: Tier): Tier = if (a.ordinal <= b.ordinal) a else b
}
