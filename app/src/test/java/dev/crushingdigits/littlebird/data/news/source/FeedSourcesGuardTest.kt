package dev.crushingdigits.littlebird.data.news.source

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser
import dev.crushingdigits.littlebird.data.news.mapper.toEntity
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every configured source MUST have a real captured fixture (feeds/source_<id>.xml) that parses
 * to at least one item with a real (non-EPOCH) date. This makes "a source silently contributes
 * nothing" impossible to ship — adding a source without a fixture, or shipping a format the
 * parser/mapper can't read, turns this red.
 */
@RunWith(RobolectricTestRunner::class)
class FeedSourcesGuardTest {
    private val parser = FeedParser()
    private val anyClassification =
        Classification(isMatch = false, category = Category.OTHER, tier = Tier.TIER_2_BEAT, confirmed = false)

    private fun fixtureOrNull(id: String) =
        javaClass.classLoader!!.getResourceAsStream("feeds/source_$id.xml")
            ?.bufferedReader()?.use { it.readText() }

    @Test fun `every configured source has a fixture that parses to at least one dated item`() {
        val failures = buildList {
            FeedSources.all.forEach { source ->
                val xml = fixtureOrNull(source.id)
                if (xml == null) {
                    add("${source.id}: no fixture (expected feeds/source_${source.id}.xml)")
                    return@forEach
                }
                val items = parser.parse(xml, source.id)
                if (items.isEmpty()) {
                    add("${source.id}: fixture parsed to 0 items")
                    return@forEach
                }
                val dated = items.map { it.toEntity(source.name, anyClassification) }
                    .count { it.publishedAtEpochMs > 0L }
                if (dated == 0) add("${source.id}: ${items.size} items but none had a non-EPOCH date")
            }
        }
        assertThat(failures).isEmpty()
    }
}
