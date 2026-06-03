package dev.crushingdigits.littlebird.data.news.mapper

import dev.crushingdigits.littlebird.data.news.datasource.local.NewsItemEntity
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val htmlTag = Regex("<[^>]+>")
private val whitespace = Regex("\\s+")

private fun String.stripHtml(): String = htmlTag.replace(this, "").replace(whitespace, " ").trim()

/**
 * RFC-1123/822 (RSS pubDate) — tolerant of single-digit days (`3 Jun`) and zone names
 * (`GMT`) as well as numeric offsets (`+0000`) — then ISO-8601 (Atom). Unknown/blank → EPOCH.
 */
private fun parseDate(raw: String?): Instant = raw?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
    runCatching { OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant() }
        .getOrNull()
} ?: Instant.EPOCH

/** Stable id so reveal-state + dedup survive refresh: guid, else link, else a synthetic hash. */
private fun stableId(raw: RawFeedItem): String =
    raw.guid?.takeIf { it.isNotBlank() }
        ?: raw.link.takeIf { it.isNotBlank() }
        ?: (raw.sourceId + "|" + raw.title + "|" + raw.pubDate).hashCode().toString()

fun RawFeedItem.toEntity(sourceName: String, classification: Classification): NewsItemEntity = NewsItemEntity(
    id = stableId(this),
    sourceId = sourceId,
    sourceName = sourceName,
    tier = classification.tier.name,
    category = classification.category.name,
    isMatch = classification.isMatch,
    confirmed = classification.confirmed,
    author = author,
    role = null,
    title = title,
    summary = description.stripHtml(),
    link = link,
    imageUrl = imageUrl,
    publishedAtEpochMs = parseDate(pubDate).toEpochMilli(),
)

fun NewsItemEntity.toDomain(): NewsItem = NewsItem(
    id = id,
    source = sourceName,
    tier = Tier.valueOf(tier),
    category = Category.valueOf(category),
    isMatch = isMatch,
    confirmed = confirmed,
    author = author,
    role = role,
    title = title,
    summary = summary,
    link = link,
    imageUrl = imageUrl,
    publishedAt = Instant.ofEpochMilli(publishedAtEpochMs),
)
