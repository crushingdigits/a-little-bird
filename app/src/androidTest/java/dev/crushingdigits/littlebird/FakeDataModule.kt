package dev.crushingdigits.littlebird

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.di.DataBindingsModule
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import javax.inject.Singleton

class FakeNewsRepository : NewsRepository {
    private val items = MutableStateFlow(
        listOf(
            NewsItem(
                id = "match-1",
                source = "BBC Sport",
                tier = Tier.TIER_2_BEAT,
                category = Category.MATCH,
                isMatch = true,
                confirmed = false,
                author = null,
                role = null,
                title = "Match result hidden under spoiler protection",
                summary = "Full-time report.",
                link = "https://x/m1",
                imageUrl = null,
                publishedAt = Instant.ofEpochMilli(2_000),
            ),
            NewsItem(
                id = "transfer-1",
                source = "Fabrizio Romano",
                tier = Tier.TIER_1_RELIABLE,
                category = Category.TRANSFER,
                isMatch = false,
                confirmed = true,
                author = null,
                role = null,
                title = "Liverpool agree £65m deal for midfielder",
                summary = "Here we go.",
                link = "https://x/t1",
                imageUrl = null,
                publishedAt = Instant.ofEpochMilli(1_000),
            ),
        ),
    )

    override fun observeFeed(): Flow<List<NewsItem>> = items
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    override suspend fun isEmpty(): Boolean = false
}

class FakePreferencesRepository : PreferencesRepository {
    private val protection = MutableStateFlow(true)
    private val revealed = MutableStateFlow<Set<String>>(emptySet())

    override fun observeProtection(): Flow<Boolean> = protection
    override fun observeRevealed(): Flow<Set<String>> = revealed
    override suspend fun setProtection(on: Boolean) { protection.value = on }
    override suspend fun toggleProtection() { protection.value = !protection.value }
    override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
    override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
    override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataBindingsModule::class])
object FakeDataModule {
    @Provides
    @Singleton
    fun newsRepository(): NewsRepository = FakeNewsRepository()

    @Provides
    @Singleton
    fun preferencesRepository(): PreferencesRepository = FakePreferencesRepository()
}
