package dev.crushingdigits.littlebird.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.news.classifier.Categoriser
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
import dev.crushingdigits.littlebird.data.news.classifier.MatchClassifier
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.classifier.defaultClassifierConfig
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.OkHttpFeedFetcher
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.news.source.FeedSources
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun feedFetcher(client: OkHttpClient): FeedFetcher = OkHttpFeedFetcher(client)

    @Provides
    @Singleton
    fun feedParser(): FeedParser = FeedParser()

    @Provides
    @Singleton
    fun newsRemoteDataSource(fetcher: FeedFetcher, parser: FeedParser) =
        NewsRemoteDataSource(fetcher, parser)

    @Provides
    @Singleton
    fun newsClassifier(): NewsClassifier = NewsClassifier(
        MatchClassifier(defaultClassifierConfig),
        Categoriser(defaultClassifierConfig),
        TierResolver(defaultTierConfig),
    )

    @Provides
    @Singleton
    fun feedSources(): List<FeedSource> = FeedSources.all

    @Provides
    @Singleton
    fun liverpoolRelevanceFilter(): LiverpoolRelevanceFilter = LiverpoolRelevanceFilter()
}
