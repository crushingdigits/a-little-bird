package dev.crushingdigits.littlebird.data.news.datasource.remote

import dev.crushingdigits.littlebird.data.news.source.FeedSource

class NewsRemoteDataSource(
    private val fetcher: FeedFetcher,
    private val parser: FeedParser,
) {
    /** Fetches + parses every source. A failed feed is skipped, not fatal. */
    suspend fun fetchAll(sources: List<FeedSource>): List<RawFeedItem> =
        sources.flatMap { source ->
            fetcher.fetch(source.url)
                .map { xml -> parser.parse(xml, source.id) }
                .getOrElse { emptyList() }
        }
}
