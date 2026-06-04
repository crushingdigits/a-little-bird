package dev.crushingdigits.littlebird.data.news.source

import dev.crushingdigits.littlebird.domain.model.Tier

enum class FeedKind { RSS, ATOM }

/**
 * One RSS/Atom source. [defaultTier] is the floor reliability for items from this feed;
 * the TierResolver may bump an individual item up (e.g. an Ornstein byline with confirmation
 * language) or down (known tabloid). [clubScoped] = false marks a transfer-wide feed (e.g.
 * Fabrizio Romano's column) whose items are filtered to Liverpool stories at ingestion.
 * Adding a feed later is a one-line addition to [FeedSources.all].
 */
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val defaultTier: Tier,
    val kind: FeedKind = FeedKind.RSS,
    val clubScoped: Boolean = true,
)
