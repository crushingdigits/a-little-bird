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
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.components.CatchUpCard
import dev.crushingdigits.littlebird.ui.components.NewsCard
import dev.crushingdigits.littlebird.ui.components.ProtectionBanner
import dev.crushingdigits.littlebird.ui.components.RedactedCard
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

/**
 * Renders [content] exactly as the Feed does: inside the dark [LittleBirdTheme] AND a
 * [Surface] painted with the theme surface color, so translucent cards and content colors
 * resolve against the real dark background (not a bare white root).
 */
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

private fun newsItem(
    tier: Tier,
    category: Category = Category.TRANSFER,
    confirmed: Boolean = false,
    isMatch: Boolean = false,
    source: String = "Fabrizio Romano",
    title: String = "Liverpool agree £65m fee for top midfield target",
    summary: String = "Exclusive: deal imminent, medical scheduled this week.",
) = NewsItem(
    id = "test-id",
    source = source,
    tier = tier,
    category = category,
    isMatch = isMatch,
    confirmed = confirmed,
    author = null,
    role = null,
    title = title,
    summary = summary,
    link = "https://example.com/news/1",
    imageUrl = null,
    publishedAt = Instant.parse("2026-06-01T10:00:00Z"),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = android.app.Application::class,
    qualifiers = RobolectricDeviceQualifiers.Pixel8a,
)
class FeedComponentsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `news card - tier1 reliable confirmed`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_news_card_tier1_reliable_confirmed.png") {
            NewsCard(
                item = newsItem(tier = Tier.TIER_1_RELIABLE, category = Category.TRANSFER, confirmed = true),
                relativeTime = "2h ago",
                onClick = {},
            )
        }
    }

    @Test
    fun `news card - tier2 beat`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_news_card_tier2_beat.png") {
            NewsCard(
                item = newsItem(
                    tier = Tier.TIER_2_BEAT,
                    category = Category.CONTRACT,
                    source = "This Is Anfield",
                    title = "Academy graduate close to signing new long-term deal",
                    summary = "Talks advancing, announcement expected soon.",
                ),
                relativeTime = "45m ago",
                onClick = {},
            )
        }
    }

    @Test
    fun `news card - tier3 speculative`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_news_card_tier3_speculative.png") {
            NewsCard(
                item = newsItem(
                    tier = Tier.TIER_3_SPECULATIVE,
                    category = Category.COACHING,
                    source = "Daily Mirror",
                    title = "Reds 'eyeing' shock swoop for La Liga forward, claim reports",
                    summary = "Speculation links the club with a surprise move.",
                ),
                relativeTime = "3h ago",
                onClick = {},
            )
        }
    }

    @Test
    fun `redacted card - match hidden`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_redacted_card_match_hidden.png") {
            RedactedCard(
                item = newsItem(
                    tier = Tier.TIER_2_BEAT,
                    category = Category.MATCH,
                    isMatch = true,
                    source = "BBC Sport",
                    title = "Match result hidden",
                    summary = "Match content redacted.",
                ),
                onClick = {},
            )
        }
    }

    @Test
    fun `catch up card - three held`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_catch_up_card_three_held.png") {
            CatchUpCard(count = 3, onMarkWatched = {})
        }
    }

    @Test
    fun `protection banner - on`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_protection_banner_on.png") {
            ProtectionBanner(on = true, onToggle = {})
        }
    }

    @Test
    fun `protection banner - off`() {
        composeTestRule.captureThemed("FeedComponentsScreenshotTest_protection_banner_off.png") {
            ProtectionBanner(on = false, onToggle = {})
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = android.app.Application::class,
    qualifiers = RobolectricDeviceQualifiers.MediumTablet,
)
class FeedComponentsTabletScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `news card tablet - tier1 reliable confirmed`() {
        composeTestRule.captureThemed("FeedComponentsTabletScreenshotTest_news_card_tablet_tier1_reliable_confirmed.png") {
            NewsCard(
                item = newsItem(tier = Tier.TIER_1_RELIABLE, category = Category.TRANSFER, confirmed = true),
                relativeTime = "2h ago",
                onClick = {},
            )
        }
    }

    @Test
    fun `protection banner tablet - on`() {
        composeTestRule.captureThemed("FeedComponentsTabletScreenshotTest_protection_banner_tablet_on.png") {
            ProtectionBanner(on = true, onToggle = {})
        }
    }
}
