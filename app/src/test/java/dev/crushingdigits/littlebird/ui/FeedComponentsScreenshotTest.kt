package dev.crushingdigits.littlebird.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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

private fun newsItem(
    tier: Tier,
    category: Category = Category.TRANSFER,
    confirmed: Boolean = false,
    isMatch: Boolean = false,
    source: String = "FabrizioRomano",
    title: String = "Arsenal agree £65m fee for top target",
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
        composeTestRule.setContent {
            LittleBirdTheme {
                NewsCard(
                    item = newsItem(
                        tier = Tier.TIER_1_RELIABLE,
                        category = Category.TRANSFER,
                        confirmed = true,
                    ),
                    relativeTime = "2h ago",
                    onClick = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_news_card_tier1_reliable_confirmed.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `news card - tier2 beat`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                NewsCard(
                    item = newsItem(
                        tier = Tier.TIER_2_BEAT,
                        category = Category.CONTRACT,
                        confirmed = false,
                        source = "The Athletic",
                        title = "Midfielder stalling on contract renewal",
                        summary = "Talks have broken down, club exploring alternatives.",
                    ),
                    relativeTime = "45m ago",
                    onClick = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_news_card_tier2_beat.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `news card - tier3 speculative`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                NewsCard(
                    item = newsItem(
                        tier = Tier.TIER_3_SPECULATIVE,
                        category = Category.COACHING,
                        confirmed = false,
                        source = "TalkSport",
                        title = "Rumours linking club with shock managerial appointment",
                        summary = "Sources claim negotiations have begun behind closed doors.",
                    ),
                    relativeTime = "3h ago",
                    onClick = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_news_card_tier3_speculative.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `redacted card - match hidden`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                RedactedCard(
                    item = newsItem(
                        tier = Tier.TIER_1_RELIABLE,
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
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_redacted_card_match_hidden.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `catch up card - three held`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                CatchUpCard(
                    count = 3,
                    onMarkWatched = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_catch_up_card_three_held.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `protection banner - on`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                ProtectionBanner(
                    on = true,
                    onToggle = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_protection_banner_on.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `protection banner - off`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                ProtectionBanner(
                    on = false,
                    onToggle = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsScreenshotTest_protection_banner_off.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
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
        composeTestRule.setContent {
            LittleBirdTheme {
                NewsCard(
                    item = NewsItem(
                        id = "tablet-test",
                        source = "FabrizioRomano",
                        tier = Tier.TIER_1_RELIABLE,
                        category = Category.TRANSFER,
                        isMatch = false,
                        confirmed = true,
                        author = null,
                        role = null,
                        title = "Arsenal agree £65m fee for top target",
                        summary = "Exclusive: deal imminent, medical scheduled this week.",
                        link = "https://example.com/news/1",
                        imageUrl = null,
                        publishedAt = Instant.parse("2026-06-01T10:00:00Z"),
                    ),
                    relativeTime = "2h ago",
                    onClick = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsTabletScreenshotTest_news_card_tablet_tier1_reliable_confirmed.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun `protection banner tablet - on`() {
        composeTestRule.setContent {
            LittleBirdTheme {
                ProtectionBanner(
                    on = true,
                    onToggle = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "FeedComponentsTabletScreenshotTest_protection_banner_tablet_on.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}
