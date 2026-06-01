package dev.crushingdigits.littlebird.domain.model

import java.time.Instant

data class NewsItem(
    val id: String,
    val source: String,
    val tier: Tier,
    val category: Category,
    val isMatch: Boolean,
    val confirmed: Boolean,
    val author: String?,
    val role: String?,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String?,
    val publishedAt: Instant,
)
