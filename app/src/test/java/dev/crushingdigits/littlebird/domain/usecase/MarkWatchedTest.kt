package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MarkWatchedTest {

    private class FakePrefs : PreferencesRepository {
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        override fun observeProtection(): Flow<Boolean> = MutableStateFlow(true)
        override fun observeRevealed(): Flow<Set<String>> = revealed
        override suspend fun setProtection(on: Boolean) {}
        override suspend fun toggleProtection() {}
        override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
        override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
        override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
    }

    @Test fun `markWatched reveals all held ids and returns prior snapshot for undo`() = runTest {
        val prefs = FakePrefs().apply { revealed.value = setOf("already") }
        val subject = MarkWatchedUseCase(prefs)

        val snapshot = subject(heldIds = setOf("m1", "m2"))

        assertThat(snapshot).containsExactly("already")
        assertThat(prefs.revealed.first()).containsExactly("already", "m1", "m2")
    }
}
