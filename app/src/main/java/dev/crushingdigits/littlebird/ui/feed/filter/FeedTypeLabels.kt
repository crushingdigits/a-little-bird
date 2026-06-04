package dev.crushingdigits.littlebird.ui.feed.filter

import dev.crushingdigits.littlebird.domain.model.FeedType

/** Short tab label for a [FeedType] (kept out of the domain enum). */
fun FeedType.shortLabel(): String = when (this) {
    FeedType.ALL -> "All"
    FeedType.TRANSFERS -> "Transfers"
    FeedType.MATCH -> "Match"
    FeedType.SQUAD_STAFF -> "Squad & staff"
    FeedType.CONTRACTS -> "Contracts"
    FeedType.ACADEMY -> "Academy"
}
