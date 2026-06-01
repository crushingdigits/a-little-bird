package dev.crushingdigits.littlebird.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.news.NewsRepositoryImpl
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepositoryImpl
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataBindingsModule {

    @Provides
    @Singleton
    fun newsRepository(
        remote: NewsRemoteDataSource,
        local: NewsLocalDataSource,
        classifier: NewsClassifier,
        sources: List<FeedSource>,
    ): NewsRepository = NewsRepositoryImpl(remote, local, classifier, sources)

    @Provides
    @Singleton
    fun preferencesRepository(source: PreferencesDataSource): PreferencesRepository =
        PreferencesRepositoryImpl(source)
}
