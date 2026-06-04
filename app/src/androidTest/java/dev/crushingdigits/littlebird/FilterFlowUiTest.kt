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
 * UIAutomator test for the news-type tab + reliability ladder flow.
 *
 * Fake feed (FakeDataModule): one T2 MATCH item (shown redacted under default protection ON) +
 * one T1 TRANSFER item. Interactions are located by testTag (viewIdResourceName), verifications by
 * displayed text. Selecting the Match feed shows the redacted match card; lowering reliability to
 * T1 excludes the T2 match item, leaving the Match feed empty (FilterEmpty); "Show all tiers"
 * restores it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FilterFlowUiTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun match_feed_at_tier1_is_empty_and_show_all_tiers_restores_it() = uiAutomator {
        startActivity(MainActivity::class.java)

        // Select the Match news-type tab (located by testTag).
        onElement(timeoutMs = 10_000L) { viewIdResourceName == "feedTab_match" }.click()

        // Default reliability is 3 -> the T2 match item shows as a redacted card.
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("Match content hidden") == true }

        // Lower reliability to T1 -> the only match item (T2) is excluded -> empty feed.
        onElement(timeoutMs = 5_000L) { viewIdResourceName == "tier_1" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Nothing here right now" }

        // "Show all tiers" resets the level to 3 -> the redacted match card returns.
        onElement(timeoutMs = 5_000L) { viewIdResourceName == "showAllTiers" }.click()
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("Match content hidden") == true }
    }
}
