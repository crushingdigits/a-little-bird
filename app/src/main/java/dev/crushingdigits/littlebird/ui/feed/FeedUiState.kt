package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Immutable snapshot of the Feed screen. Modal state (gate/catch-up) lives here so it
 *  survives configuration changes; one-off effects (toast) flow via [FeedAction]. */
data class FeedUiState(
    val cards: List<FeedCard> = emptyList(),
    val protectionOn: Boolean = true,
    val heldMatchCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val isEmpty: Boolean = false,
    val gateTarget: NewsItem? = null,
    val catchUpVisible: Boolean = false,
)
