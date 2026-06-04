package dev.crushingdigits.littlebird.data.news.tier

data class TierConfig(
    val tier1Journalists: Set<String>,
    val tier3Sources: Set<String>,
    val confirmationPhrases: Set<String>,
    val tentativePhrases: Set<String>,
)

val defaultTierConfig = TierConfig(
    tier1Journalists = setOf("david ornstein", "fabrizio romano", "david lynch"),
    tier3Sources = setOf("daily mirror", "the sun", "the mirror", "mirror", "sun"),
    // Multi-word phrases only — keeps "win confirmed" / "official website" from false-firing.
    confirmationPhrases = setOf(
        "here we go",
        "done deal",
        "deal done",
        "deal agreed",
        "agreement reached",
        "medical completed",
        "officially signed",
        "transfer confirmed",
        "deal confirmed",
        "completed the signing",
        "deal sealed",
    ),
    tentativePhrases = setOf(
        "interested in",
        "keen on",
        "linked with",
        "in talks",
        "monitoring",
        "tracking",
        "eyeing",
        "considering",
        "reportedly",
        "could sign",
        "target",
        "rumour",
        "rumor",
    ),
)
