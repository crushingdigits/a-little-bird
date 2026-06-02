package dev.crushingdigits.littlebird.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.detail.DetailContent
import dev.crushingdigits.littlebird.ui.detail.DetailMessage
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

private val DefaultRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
)

private fun ComposeContentTestRule.captureThemed(filePath: String, content: @Composable () -> Unit) {
    setContent {
        LittleBirdTheme {
            Surface(color = MaterialTheme.colorScheme.surface) { Box(Modifier) { content() } }
        }
    }
    onRoot().captureRoboImage(filePath = filePath, roborazziOptions = DefaultRoborazziOptions)
}

private fun item(
    tier: Tier,
    confirmed: Boolean = false,
    author: String? = "David Ornstein",
    role: String? = "The Athletic",
    summary: String = "Liverpool have reached full agreement on personal terms with their priority midfield target.",
    imageUrl: String? = null,
) = NewsItem(
    id = "id", source = "The Athletic", tier = tier, category = Category.TRANSFER, isMatch = false,
    confirmed = confirmed, author = author, role = role,
    title = "Liverpool agree personal terms with target midfielder",
    summary = summary, link = "https://example.com/1", imageUrl = imageUrl,
    publishedAt = Instant.parse("2026-06-01T10:00:00Z"),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel8a)
class DetailScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `detail - tier1 confirmed`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier1_confirmed.png") {
            DetailContent(item(Tier.TIER_1_RELIABLE, confirmed = true), "2h", {}, {}, {})
        }
    }

    @Test fun `detail - tier2 no image`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier2.png") {
            DetailContent(item(Tier.TIER_2_BEAT, author = "This Is Anfield", role = "Beat reporting"), "45m", {}, {}, {})
        }
    }

    @Test fun `detail - tier3 speculative`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier3.png") {
            DetailContent(item(Tier.TIER_3_SPECULATIVE, author = "Daily Mirror", role = "Aggregated speculation"), "3h", {}, {}, {})
        }
    }

    @Test fun `detail - blank summary fallback`() {
        composeTestRule.captureThemed("DetailScreenshotTest_blank_summary.png") {
            DetailContent(item(Tier.TIER_2_BEAT, summary = ""), "1h", {}, {}, {})
        }
    }

    @Test fun `detail - with image slot`() {
        composeTestRule.captureThemed("DetailScreenshotTest_with_image.png") {
            DetailContent(item(Tier.TIER_2_BEAT, imageUrl = "https://example.com/p.jpg"), "20m", {}, {}, {})
        }
    }

    @Test fun `detail - not found state`() {
        composeTestRule.captureThemed("DetailScreenshotTest_not_found.png") {
            DetailMessage("Story unavailable", "It may have dropped from the latest feed.", onBack = {})
        }
    }
}
