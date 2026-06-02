package dev.crushingdigits.littlebird.ui.detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DetailActionsTest {

    /** Build a minimal Activity context so startActivity() doesn't need FLAG_ACTIVITY_NEW_TASK. */
    private fun activityContext(): Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun item(link: String = "https://example.com/story") = NewsItem(
        id = "test-id",
        source = "The Athletic",
        tier = Tier.TIER_1_RELIABLE,
        category = Category.TRANSFER,
        isMatch = false,
        confirmed = false,
        author = "David Ornstein",
        role = "The Athletic",
        title = "Liverpool agree deal for midfielder",
        summary = "Full agreement reached on personal terms.",
        link = link,
        imageUrl = null,
        publishedAt = Instant.ofEpochMilli(1_000_000),
    )

    @Test fun `shareItem with non-blank link starts ACTION_CHOOSER wrapping ACTION_SEND with title and link`() {
        val ctx = activityContext()
        val testItem = item(link = "https://example.com/story")

        shareItem(ctx, testItem)

        val started = shadowOf(ctx).nextStartedActivity
        assertThat(started).isNotNull()
        assertThat(started.action).isEqualTo(Intent.ACTION_CHOOSER)

        @Suppress("DEPRECATION")
        val wrapped = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(wrapped).isNotNull()
        assertThat(wrapped!!.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(wrapped.type).isEqualTo("text/plain")
        assertThat(wrapped.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo(testItem.title)
        val extraText = wrapped.getStringExtra(Intent.EXTRA_TEXT)!!
        assertThat(extraText).contains(testItem.title)
        assertThat(extraText).contains(testItem.link)
    }

    @Test fun `shareItem with blank link sends only the title in EXTRA_TEXT`() {
        val ctx = activityContext()
        val testItem = item(link = "")

        shareItem(ctx, testItem)

        val started = shadowOf(ctx).nextStartedActivity
        assertThat(started).isNotNull()
        assertThat(started.action).isEqualTo(Intent.ACTION_CHOOSER)

        @Suppress("DEPRECATION")
        val wrapped = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(wrapped).isNotNull()
        val extraText = wrapped!!.getStringExtra(Intent.EXTRA_TEXT)!!
        assertThat(extraText).isEqualTo(testItem.title)
    }

    @Test fun `openOriginal starts an activity whose intent data matches the URL`() {
        val ctx = activityContext()
        val url = "https://example.com/article"

        openOriginal(ctx, url)

        val started = shadowOf(ctx).nextStartedActivity
        assertThat(started).isNotNull()
        assertThat(started.data).isEqualTo(Uri.parse(url))
    }
}
