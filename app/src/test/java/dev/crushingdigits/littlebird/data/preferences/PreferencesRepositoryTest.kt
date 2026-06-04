package dev.crushingdigits.littlebird.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var subject: PreferencesRepositoryImpl

    @Before fun setUp() {
        val dataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(produceFile = { tmp.newFile("prefs.preferences_pb") })
        subject = PreferencesRepositoryImpl(PreferencesDataSource(dataStore))
    }

    @Test fun `protection defaults on and toggles`() = runTest {
        assertThat(subject.observeProtection().first()).isTrue()
        subject.toggleProtection()
        assertThat(subject.observeProtection().first()).isFalse()
    }

    @Test fun `reveal adds id and revealAll adds many`() = runTest {
        subject.reveal("a")
        subject.revealAll(setOf("b", "c"))
        assertThat(subject.observeRevealed().first()).containsExactly("a", "b", "c")
    }

    @Test fun `restoreRevealed replaces the whole set`() = runTest {
        subject.revealAll(setOf("a", "b"))
        subject.restoreRevealed(setOf("only"))
        assertThat(subject.observeRevealed().first()).containsExactly("only")
    }

    @Test fun `setProtection sets explicit value`() = runTest {
        subject.setProtection(false)
        assertThat(subject.observeProtection().first()).isFalse()
    }

    @Test fun `reliability level defaults to 3 and persists`() = runTest {
        assertThat(subject.observeReliabilityLevel().first()).isEqualTo(3)
        subject.setReliabilityLevel(1)
        assertThat(subject.observeReliabilityLevel().first()).isEqualTo(1)
    }

    @Test fun `pinned feed is absent by default, sets and clears`() = runTest {
        assertThat(subject.observePinnedFeed().first()).isNull()
        subject.setPinnedFeed("transfer")
        assertThat(subject.observePinnedFeed().first()).isEqualTo("transfer")
        subject.setPinnedFeed(null)
        assertThat(subject.observePinnedFeed().first()).isNull()
    }
}
