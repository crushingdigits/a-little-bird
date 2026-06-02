package dev.crushingdigits.littlebird.ui.util

import dev.crushingdigits.littlebird.domain.model.NewsItem
import java.time.Duration
import java.time.Instant

/** Compact relative age ("now", "5m", "3h", "2d"). The Detail reader renders it as "{value} ago". */
internal fun NewsItem.relativeTime(now: Instant = Instant.now()): String {
    val mins = Duration.between(publishedAt, now).toMinutes().coerceAtLeast(0)
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}
