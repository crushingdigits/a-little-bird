package dev.crushingdigits.littlebird

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UIAutomator tests for the per-game spoiler wall.
 *
 * Fakes (FakeDataModule): one played game "Chelsea" (wall) + a match news item published after it
 * (hidden) and a transfer item before it (visible). Protection is ON by default.
 *
 * NOTE: COMPILED but RUN on a device via connectedDebugAndroidTest (Task 12.3).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SpoilerFlowUiTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    /** A story after the wall renders redacted; tapping opens the (date-based) gate; reveal opens the reader. */
    @Test
    fun hidden_story_can_be_revealed_through_the_gate() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement(timeoutMs = 10_000L) { textAsString() == "Hidden until you catch up" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "This one might spoil you" }
        onElement(timeoutMs = 5_000L) { textAsString() == "Reveal anyway" }.click()
        // After reveal, the reader opens (assert its button; don't click — avoids leaving the app).
        onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }
    }

    /** Opening the fixtures sheet from the protection card and marking the wall game watched shows a toast. */
    @Test
    fun marking_a_game_watched_releases_held_stories() = uiAutomator {
        startActivity(MainActivity::class.java)

        // The protection card's action row opens the sheet.
        onElement(timeoutMs = 10_000L) { textAsString()?.contains("Manage") == true }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Fixtures & spoilers" }
        onElement(timeoutMs = 5_000L) { textAsString() == "Mark watched" }.click()
        // Toast confirms release.
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("unlocked") == true }
    }

    /** A non-match (before-wall) card opens the reader directly. */
    @Test
    fun tapping_a_visible_card_opens_the_detail_reader() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement(timeoutMs = 10_000L) { textAsString() == "Liverpool agree £65m deal for midfielder" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }
        pressBack()
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("Spoiler protection") == true }
    }
}
