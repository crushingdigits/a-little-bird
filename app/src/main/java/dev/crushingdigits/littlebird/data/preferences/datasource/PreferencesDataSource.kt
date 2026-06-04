package dev.crushingdigits.littlebird.data.preferences.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataSource(private val dataStore: DataStore<Preferences>) {

    private val protectionOnKey = booleanPreferencesKey("protection_on")
    private val revealedIdsKey = stringSetPreferencesKey("revealed_ids")
    private val reliabilityLevelKey = intPreferencesKey("reliability_level")
    private val pinnedFeedKey = stringPreferencesKey("pinned_feed")

    val protectionOn: Flow<Boolean> = dataStore.data.map { it[protectionOnKey] ?: true } // default ON
    val revealedIds: Flow<Set<String>> = dataStore.data.map { it[revealedIdsKey] ?: emptySet() }
    val reliabilityLevel: Flow<Int> = dataStore.data.map { it[reliabilityLevelKey] ?: 3 } // default 3 = all tiers
    val pinnedFeed: Flow<String?> = dataStore.data.map { it[pinnedFeedKey] }

    suspend fun setProtectionOn(on: Boolean) { dataStore.edit { it[protectionOnKey] = on } }
    suspend fun setRevealedIds(ids: Set<String>) { dataStore.edit { it[revealedIdsKey] = ids } }
    suspend fun setReliabilityLevel(level: Int) { dataStore.edit { it[reliabilityLevelKey] = level } }
    suspend fun setPinnedFeed(key: String?) {
        dataStore.edit { if (key == null) it.remove(pinnedFeedKey) else it[pinnedFeedKey] = key }
    }
}
