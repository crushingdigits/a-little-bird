package dev.crushingdigits.littlebird.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.feed.FeedTab
import dev.crushingdigits.littlebird.ui.feed.filter.FeedTypeTabs
import dev.crushingdigits.littlebird.ui.feed.filter.FilterEmpty
import dev.crushingdigits.littlebird.ui.feed.filter.PinButton
import dev.crushingdigits.littlebird.ui.feed.filter.ReliabilityLadder
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val DefaultRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
)

private fun ComposeContentTestRule.captureThemed(filePath: String, content: @Composable () -> Unit) {
    setContent {
        LittleBirdTheme {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.padding(16.dp)) { content() }
            }
        }
    }
    onRoot().captureRoboImage(filePath = filePath, roborazziOptions = DefaultRoborazziOptions)
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel8a)
class FilterComponentsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `reliability ladder - level 1`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level1.png") {
            ReliabilityLadder(selected = Tier.TIER_1_RELIABLE, onSelect = {})
        }
    }

    @Test fun `reliability ladder - level 2`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level2.png") {
            ReliabilityLadder(selected = Tier.TIER_2_BEAT, onSelect = {})
        }
    }

    @Test fun `reliability ladder - level 3`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level3.png") {
            ReliabilityLadder(selected = Tier.TIER_3_SPECULATIVE, onSelect = {})
        }
    }

    private val sampleTabs = listOf(
        FeedTab(FeedType.ALL, 12),
        FeedTab(FeedType.TRANSFERS, 5),
        FeedTab(FeedType.MATCH, 0),
        FeedTab(FeedType.SQUAD_STAFF, 3),
        FeedTab(FeedType.CONTRACTS, 2),
        FeedTab(FeedType.ACADEMY, 2),
    )

    @Test fun `type tabs - all selected`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_type_tabs_all_selected.png") {
            FeedTypeTabs(tabs = sampleTabs, selected = FeedType.ALL, pinned = null, onSelect = {})
        }
    }

    @Test fun `type tabs - transfers selected and pinned`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_type_tabs_transfers_pinned.png") {
            FeedTypeTabs(tabs = sampleTabs, selected = FeedType.TRANSFERS, pinned = FeedType.TRANSFERS, onSelect = {})
        }
    }

    @Test fun `pin button - on`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_pin_button_on.png") {
            PinButton(pinned = true, onToggle = {})
        }
    }

    @Test fun `pin button - off`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_pin_button_off.png") {
            PinButton(pinned = false, onToggle = {})
        }
    }

    @Test fun `filter empty state`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_filter_empty.png") {
            FilterEmpty(onShowAllTiers = {})
        }
    }
}
