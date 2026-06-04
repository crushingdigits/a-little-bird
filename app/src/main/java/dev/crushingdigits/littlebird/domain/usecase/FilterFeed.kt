package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.FilteredFeed
import dev.crushingdigits.littlebird.domain.model.NewsItem
import javax.inject.Inject

fun interface FilterFeed {
    operator fun invoke(items: List<NewsItem>, filter: FeedFilter): FilteredFeed
}

internal class FilterFeedUseCase @Inject constructor() : FilterFeed {
    override fun invoke(items: List<NewsItem>, filter: FeedFilter): FilteredFeed {
        // Cumulative reliability ceiling: tier ordinal 0 (RELIABLE) .. 2 (SPECULATIVE).
        val withinTier = items.filter { it.tier.ordinal <= filter.maxTier.ordinal }
        val visible = withinTier.filter { filter.type.matches(it.category) }
        val counts = FeedType.entries.associateWith { type ->
            withinTier.count { type.matches(it.category) }
        }
        return FilteredFeed(visible = visible, counts = counts)
    }
}
