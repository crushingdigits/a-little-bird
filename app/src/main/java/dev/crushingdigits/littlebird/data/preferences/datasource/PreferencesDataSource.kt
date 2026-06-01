package dev.crushingdigits.littlebird.data.preferences.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataSource(private val dataStore: DataStore<Preferences>) {

    private val protectionOnKey = booleanPreferencesKey("protection_on")
    private val revealedIdsKey = stringSetPreferencesKey("revealed_ids")

    val protectionOn: Flow<Boolean> = dataStore.data.map { it[protectionOnKey] ?: true } // default ON
    val revealedIds: Flow<Set<String>> = dataStore.data.map { it[revealedIdsKey] ?: emptySet() }

    suspend fun setProtectionOn(on: Boolean) { dataStore.edit { it[protectionOnKey] = on } }
    suspend fun setRevealedIds(ids: Set<String>) { dataStore.edit { it[revealedIdsKey] = ids } }
}
