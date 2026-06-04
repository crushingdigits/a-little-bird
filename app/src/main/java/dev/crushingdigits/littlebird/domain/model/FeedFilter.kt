package dev.crushingdigits.littlebird.domain.model

/** The active filter selection: which feed [type] and the cumulative reliability ceiling [maxTier]. */
data class FeedFilter(val type: FeedType, val maxTier: Tier)

/** Result of applying a [FeedFilter]: the [visible] items plus per-type [counts] at the current ceiling. */
data class FilteredFeed(val visible: List<NewsItem>, val counts: Map<FeedType, Int>)
