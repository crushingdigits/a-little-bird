package dev.crushingdigits.littlebird.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Open the article in a Chrome Custom Tab; fall back to a plain VIEW intent if none is available. */
internal fun openOriginal(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

/** Native share sheet with the headline + link. */
internal fun shareItem(context: Context, item: NewsItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.link}")
    }
    context.startActivity(Intent.createChooser(send, null))
}
