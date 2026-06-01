package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.NewsItem

/** How a single feed item should render: normally, or redacted (held match content). */
sealed interface FeedCard {
    val item: NewsItem

    data class Normal(override val item: NewsItem) : FeedCard
    data class Redacted(override val item: NewsItem) : FeedCard
}
