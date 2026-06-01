package dev.crushingdigits.littlebird.data.news.datasource.local

import kotlinx.coroutines.flow.Flow

class NewsLocalDataSource(private val dao: NewsDao) {
    fun observeAll(): Flow<List<NewsItemEntity>> = dao.observeAll()
    suspend fun upsert(items: List<NewsItemEntity>) = dao.upsertAll(items)
    suspend fun count(): Int = dao.count()
}
