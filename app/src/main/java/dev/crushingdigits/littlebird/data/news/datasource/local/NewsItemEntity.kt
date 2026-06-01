package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_items")
data class NewsItemEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceName: String,
    val tier: String,        // Tier.name
    val category: String,    // Category.name
    val isMatch: Boolean,
    val confirmed: Boolean,
    val author: String?,
    val role: String?,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String?,
    val publishedAtEpochMs: Long,
)
