package dev.crushingdigits.littlebird.data.preferences

import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface PreferencesRepository {
    fun observeProtection(): Flow<Boolean>
    fun observeRevealed(): Flow<Set<String>>
    suspend fun setProtection(on: Boolean)
    suspend fun toggleProtection()
    suspend fun reveal(id: String)
    suspend fun revealAll(ids: Set<String>)
    suspend fun restoreRevealed(ids: Set<String>)
    fun observeReliabilityLevel(): Flow<Int>
    fun observePinnedFeed(): Flow<String?>
    suspend fun setReliabilityLevel(level: Int)
    suspend fun setPinnedFeed(key: String?)
}

class PreferencesRepositoryImpl(private val source: PreferencesDataSource) : PreferencesRepository {
    override fun observeProtection(): Flow<Boolean> = source.protectionOn
    override fun observeRevealed(): Flow<Set<String>> = source.revealedIds
    override suspend fun setProtection(on: Boolean) = source.setProtectionOn(on)
    override suspend fun toggleProtection() = source.setProtectionOn(!source.protectionOn.first())
    override suspend fun reveal(id: String) = source.setRevealedIds(source.revealedIds.first() + id)
    override suspend fun revealAll(ids: Set<String>) = source.setRevealedIds(source.revealedIds.first() + ids)
    override suspend fun restoreRevealed(ids: Set<String>) = source.setRevealedIds(ids)
    override fun observeReliabilityLevel(): Flow<Int> = source.reliabilityLevel
    override fun observePinnedFeed(): Flow<String?> = source.pinnedFeed
    override suspend fun setReliabilityLevel(level: Int) = source.setReliabilityLevel(level)
    override suspend fun setPinnedFeed(key: String?) = source.setPinnedFeed(key)
}
