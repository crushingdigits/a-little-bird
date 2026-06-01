package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import org.junit.Test

class CategoriserTest {
    private val subject = Categoriser(defaultClassifierConfig)

    @Test fun `match content is MATCH regardless of keywords`() {
        assertThat(subject.categorise("Liverpool sign new XI match report", isMatch = true))
            .isEqualTo(Category.MATCH)
    }

    @Test fun `transfer wins by priority`() {
        assertThat(subject.categorise("Liverpool agree fee to sign midfielder", isMatch = false))
            .isEqualTo(Category.TRANSFER)
    }

    @Test fun `coaching detected`() {
        assertThat(subject.categorise("Club appoints new assistant manager", isMatch = false))
            .isEqualTo(Category.COACHING)
    }

    @Test fun `academy detected`() {
        assertThat(subject.categorise("U18 graduate promoted to first team", isMatch = false))
            .isEqualTo(Category.ACADEMY)
    }

    @Test fun `no keyword hit falls back to OTHER`() {
        assertThat(subject.categorise("Anfield atmosphere feature piece", isMatch = false))
            .isEqualTo(Category.OTHER)
    }
}
