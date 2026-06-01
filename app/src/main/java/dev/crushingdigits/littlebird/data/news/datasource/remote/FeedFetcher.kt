package dev.crushingdigits.littlebird.data.news.datasource.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Downloads a feed's raw XML. Main-safe; returns a Result rather than throwing. */
interface FeedFetcher {
    suspend fun fetch(url: String): Result<String>
}

class OkHttpFeedFetcher(private val client: OkHttpClient) : FeedFetcher {
    override suspend fun fetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", "ALittleBird/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
                response.body?.string() ?: throw IOException("Empty body for $url")
            }
        }
    }
}
