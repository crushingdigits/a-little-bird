package dev.crushingdigits.littlebird.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Open the article in a Chrome Custom Tab; fall back to a plain VIEW intent if none is available.
 *  If both fail (no app can handle the link), surfaces a Toast instead of a silent no-op. */
internal fun openOriginal(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Native share sheet with the headline + link. Omits the link when it is blank (guid-only items). */
internal fun shareItem(context: Context, item: NewsItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, if (item.link.isNotBlank()) "${item.title}\n${item.link}" else item.title)
    }
    context.startActivity(Intent.createChooser(send, null))
}
