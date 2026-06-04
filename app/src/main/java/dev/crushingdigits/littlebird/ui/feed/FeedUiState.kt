package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier

/** One news-type tab: its [type] and the live item [count] at the current reliability level. */
data class FeedTab(val type: FeedType, val count: Int)

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
    // Filter chrome
    val filterTabs: List<FeedTab> = emptyList(),
    val selectedType: FeedType = FeedType.ALL,
    val reliabilityLevel: Tier = Tier.TIER_3_SPECULATIVE,
    val pinnedType: FeedType? = null,
    val isFilteredEmpty: Boolean = false,
)
