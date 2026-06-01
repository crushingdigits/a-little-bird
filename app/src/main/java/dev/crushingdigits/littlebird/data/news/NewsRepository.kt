package dev.crushingdigits.littlebird.data.news

import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.mapper.toDomain
import dev.crushingdigits.littlebird.data.news.mapper.toEntity
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NewsRepository {
    fun observeFeed(): Flow<List<NewsItem>>
    suspend fun refresh(): Result<Unit>
    suspend fun isEmpty(): Boolean
}

class NewsRepositoryImpl(
    private val remote: NewsRemoteDataSource,
    private val local: NewsLocalDataSource,
    private val classifier: NewsClassifier,
    private val sources: List<FeedSource>,
) : NewsRepository {

    private val sourcesById = sources.associateBy { it.id }

    override fun observeFeed(): Flow<List<NewsItem>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val raw = remote.fetchAll(sources)
        val entities = raw.map { item ->
            val src = sourcesById[item.sourceId]
            val classification = classifier.classify(
                title = item.title,
                description = item.description,
                author = item.author,
                defaultTier = src?.defaultTier ?: Tier.TIER_3_SPECULATIVE,
            )
            item.toEntity(sourceName = src?.name ?: item.sourceId, classification = classification)
        }
        if (entities.isNotEmpty()) local.upsert(entities)
    }

    override suspend fun isEmpty(): Boolean = local.count() == 0
}
