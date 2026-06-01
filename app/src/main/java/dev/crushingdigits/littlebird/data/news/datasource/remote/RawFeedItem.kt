package dev.crushingdigits.littlebird.data.news.datasource.remote

data class RawFeedItem(
    val sourceId: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String?,
    val author: String?,
    val categories: List<String>,
    val imageUrl: String?,
    val guid: String?,
)
