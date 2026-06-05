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
import dev.crushingdigits.littlebird.domain.model.MatchResult
import dev.crushingdigits.littlebird.ui.components.ProtectionCard
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import dev.crushingdigits.littlebird.ui.feed.wall.FixtureRow
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
class ScheduleComponentsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val ko = Instant.parse("2026-06-01T16:00:00Z")

    private fun fixture(state: FixtureState, score: String? = null, result: MatchResult? = null, isWall: Boolean = false) = FixtureUi(
        id = "g", opponent = "Chelsea", abbreviation = "CHE", isHome = true, competition = "English Premier League",
        kickoff = ko, state = state, scoreLine = score, result = result, isWall = isWall,
    )

    @Test fun `protection card - off`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_off.png") {
            ProtectionCard(on = false, hidingCount = 0, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `protection card - hiding`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_hiding.png") {
            ProtectionCard(on = true, hidingCount = 2, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `protection card - caught up`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_caught_up.png") {
            ProtectionCard(on = true, hidingCount = 0, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `fixture row - watched shows score`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_watched.png") {
            FixtureRow(fixture(FixtureState.WATCHED, score = "2–1", result = MatchResult.WIN), onMarkWatched = {}, onUnmark = {})
        }
    }

    @Test fun `fixture row - unwatched hides score and is the wall`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_unwatched.png") {
            FixtureRow(fixture(FixtureState.UNWATCHED, isWall = true), onMarkWatched = {}, onUnmark = {})
        }
    }

    @Test fun `fixture row - upcoming`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_upcoming.png") {
            FixtureRow(fixture(FixtureState.UPCOMING), onMarkWatched = {}, onUnmark = {})
        }
    }
}
