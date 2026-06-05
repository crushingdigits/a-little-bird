package dev.crushingdigits.littlebird.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.news.NewsRepositoryImpl
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepositoryImpl
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepositoryImpl
import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
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
        relevanceFilter: LiverpoolRelevanceFilter,
    ): NewsRepository = NewsRepositoryImpl(remote, local, classifier, sources, relevanceFilter)

    @Provides
    @Singleton
    fun preferencesRepository(source: PreferencesDataSource): PreferencesRepository =
        PreferencesRepositoryImpl(source)

    @Provides
    @Singleton
    fun scheduleRepository(remote: ScheduleDataSource, local: ScheduleLocalDataSource): ScheduleRepository =
        ScheduleRepositoryImpl(remote, local)
}
