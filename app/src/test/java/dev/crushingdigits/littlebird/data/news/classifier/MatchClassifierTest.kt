package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchClassifierTest {

    private val subject = MatchClassifier(defaultClassifierConfig)

    // Headlines that MUST be flagged as match content (zero leaks allowed).
    private val matchHeadlines = listOf(
        "Liverpool 2-1 Arsenal: full-time report and player ratings",
        "Player ratings: who shone and who struggled",
        "Match report: Reds edge thriller at Anfield",
        "Liverpool vs Chelsea: live updates as it happened",
        "Half-time: Liverpool trail after early goal",
        "Predicted XI and team news ahead of kick-off",
        "WATCH: all the goals from a 3–3 draw",
        "Salah penalty seals win over rivals",
        "Highlights: Liverpool come from behind",
        "Liverpool v Everton preview",
        "Red card overshadows derby defeat",
    )

    // Headlines that MUST NOT be flagged (transfer/coaching news).
    private val nonMatchHeadlines = listOf(
        "Liverpool agree personal terms with target midfielder",
        "New set-piece coach expected to join the backroom staff",
        "Academy graduate signs new long-term deal",
        "Reds 'eyeing' shock swoop for La Liga forward, claim reports",
        "New head of fitness lined up in staff reshuffle",
        "Fabrizio Romano: here we go for £40m midfield deal",
        "Liverpool complete signing of left-back on five-year contract",
        "Club confirm appointment of new assistant manager",
    )

    @Test
    fun `flags all match headlines - 100 percent recall`() {
        val leaks = matchHeadlines.filterNot { subject.isMatch(it) }
        assertThat(leaks).isEmpty()
    }

    @Test
    fun `does not flag transfer or coaching headlines`() {
        val falsePositives = nonMatchHeadlines.filter { subject.isMatch(it) }
        assertThat(falsePositives).isEmpty()
    }

    @Test
    fun `detects scorelines with en-dash and hyphen`() {
        assertThat(subject.isMatch("Reds win 2-0")).isTrue()
        assertThat(subject.isMatch("Thriller ends 3–3")).isTrue()
    }

    @Test
    fun `bare token v and ft only match on word boundaries`() {
        assertThat(subject.isMatch("Liverpool v Everton")).isTrue()
        assertThat(subject.isMatch("Transfer of the season")).isFalse() // 'ft' inside 'transfer' must not match
    }

    @Test
    fun `a non-match override cancels a weak signal hit but never a scoreline`() {
        val classifier = MatchClassifier(defaultClassifierConfig.copy(nonMatchOverrides = listOf("transfer saga")))
        // "win" is a match signal, but the transfer-context override cancels the weak hit.
        assertThat(classifier.isMatch("Liverpool win bidding war in transfer saga")).isFalse()
        // A scoreline is unconditional — an override never cancels it.
        assertThat(classifier.isMatch("Transfer saga rumbles on after 2-0 result")).isTrue()
    }
}
