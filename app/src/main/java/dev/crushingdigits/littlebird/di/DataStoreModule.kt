package dev.crushingdigits.littlebird.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "little_bird_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides
    @Singleton
    fun preferencesDataSource(ds: DataStore<Preferences>) = PreferencesDataSource(ds)
}
