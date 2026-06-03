package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiverpoolRelevanceFilterTest {
    private val subject = LiverpoolRelevanceFilter()

    @Test fun `keeps an item mentioning Liverpool in the title`() {
        assertThat(subject.isRelevant("Liverpool agree deal for midfielder", "Personal terms done")).isTrue()
    }

    @Test fun `keeps an item mentioning Anfield in the description`() {
        assertThat(subject.isRelevant("Big summer ahead", "Plans are taking shape at Anfield")).isTrue()
    }

    @Test fun `drops an item about a different club`() {
        assertThat(subject.isRelevant("Arsenal eye Declan Rice", "Arteta wants a new deal at the Emirates")).isFalse()
    }

    @Test fun `matching is case-insensitive`() {
        assertThat(subject.isRelevant("LIVERPOOL CLOSING IN", "")).isTrue()
    }
}
