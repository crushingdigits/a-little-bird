package dev.crushingdigits.littlebird.data.news.source

import dev.crushingdigits.littlebird.domain.model.Tier

/**
 * The editable seed list of feeds. URLs are verified at build time (see spec §12);
 * the Liverpool Echo (Reach platform) URL uses the `?service=rss` suffix convention.
 */
object FeedSources {
    val all: List<FeedSource> = listOf(
        FeedSource(
            id = "this_is_anfield",
            name = "This Is Anfield",
            url = "https://www.thisisanfield.com/feed/",
            defaultTier = Tier.TIER_2_BEAT,
        ),
        FeedSource(
            id = "liverpool_echo",
            name = "Liverpool Echo",
            url = "https://www.liverpool.com/all-about/liverpool-fc?service=rss",
            defaultTier = Tier.TIER_2_BEAT,
        ),
        FeedSource(
            id = "bbc_liverpool",
            name = "BBC Sport — Liverpool",
            url = "https://feeds.bbci.co.uk/sport/football/teams/liverpool/rss.xml",
            defaultTier = Tier.TIER_1_RELIABLE,
        ),
        FeedSource(
            id = "guardian_liverpool",
            name = "The Guardian — Liverpool",
            url = "https://www.theguardian.com/football/liverpool/rss",
            defaultTier = Tier.TIER_1_RELIABLE,
        ),
    )
}
