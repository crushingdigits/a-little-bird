package dev.crushingdigits.littlebird.data.news.tier

data class TierConfig(
    val tier1Journalists: Set<String>,
    val tier3Sources: Set<String>,
    val confirmedPhrase: String,
)

val defaultTierConfig = TierConfig(
    tier1Journalists = setOf("david ornstein", "fabrizio romano"),
    tier3Sources = setOf("daily mirror", "the sun", "the mirror", "mirror", "sun"),
    confirmedPhrase = "here we go",
)
