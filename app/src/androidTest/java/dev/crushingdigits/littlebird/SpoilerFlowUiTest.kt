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
 * End-to-end UIAutomator tests for the spoiler-protection signature flow.
 *
 * The Hilt test harness (FakeDataModule / FakePreferencesRepository) feeds the app a deterministic
 * feed of two items — one MATCH item (protection ON by default → shown as RedactedCard) and one
 * TRANSFER item. Tests verify the UI transitions that a real user would see.
 *
 * The [onElement] predicate block is an extension on AccessibilityNodeInfo, so helper extensions
 * like [textAsString] are called without a receiver prefix. The [uiAutomator] block is an
 * extension on UiAutomatorTestScope, so methods like [startActivity] and [onElement] are called
 * directly.
 *
 * NOTE: COMPILED but NOT yet RUN on a device. Run via connectedDebugAndroidTest in Task 1B.11.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SpoilerFlowUiTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    /**
     * With spoiler protection ON (the fake default), the MATCH item is shown as a RedactedCard
     * displaying "Match content hidden". Tapping it opens the GateSheet which says
     * "This one might spoil you". Tapping "Reveal anyway" dismisses the sheet and reveals the item.
     */
    @Test
    fun protected_match_card_can_be_revealed_through_the_gate() = uiAutomator {
        startActivity(MainActivity::class.java)

        // Wait generously (10 s) for app launch + initial state emission.
        // The RedactedCard has a Text composable with "Match content hidden".
        val redactedCard = onElement(timeoutMs = 10_000L) {
            textAsString() == "Match content hidden"
        }
        redactedCard.click()

        // GateSheet should appear with its title.
        onElement(timeoutMs = 5_000L) {
            textAsString() == "This one might spoil you"
        }

        // Tap "Reveal anyway" — the primary button on the GateSheet.
        val revealButton = onElement(timeoutMs = 5_000L) {
            textAsString() == "Reveal anyway"
        }
        revealButton.click()

        // After reveal the GateSheet is gone and the feed is visible.
        // The ProtectionBanner with "Spoiler protection" text confirms we are back on the feed.
        onElement(timeoutMs = 5_000L) {
            textAsString()?.contains("Spoiler protection") == true
        }
    }

    /**
     * With protection ON and the MATCH item held, the CatchUpCard shows "Mark match as watched".
     * Tapping it opens the CatchUpSheet ("All caught up?"). Tapping "Yes, reveal them" triggers
     * onMarkWatched which emits a ShowToast action; the Snackbar message contains
     * "match updates unlocked".
     */
    @Test
    fun mark_match_as_watched_releases_held_updates() = uiAutomator {
        startActivity(MainActivity::class.java)

        // CatchUpCard appears when protectionOn=true and heldMatchCount > 0.
        val markWatchedButton = onElement(timeoutMs = 10_000L) {
            textAsString() == "Mark match as watched"
        }
        markWatchedButton.click()

        // CatchUpSheet title.
        onElement(timeoutMs = 5_000L) {
            textAsString() == "All caught up?"
        }

        // Confirm button on the CatchUpSheet.
        val confirmButton = onElement(timeoutMs = 5_000L) {
            textAsString() == "Yes, reveal them"
        }
        confirmButton.click()

        // Snackbar message — FeedViewModel emits "1 match updates unlocked".
        onElement(timeoutMs = 5_000L) {
            textAsString()?.contains("match updates unlocked") == true
        }
    }
}
