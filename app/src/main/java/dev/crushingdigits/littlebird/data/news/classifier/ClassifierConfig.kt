package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.domain.model.Category

data class ClassifierConfig(
    val matchSignals: List<String>,
    val scorelineRegex: Regex,
    val transferContextAllowlist: List<String>,
    /** Ordered by priority: first category with a hit wins. */
    val categoryKeywords: List<Pair<Category, List<String>>>,
)

val defaultClassifierConfig = ClassifierConfig(
    matchSignals = listOf(
        "full-time", "full time", "half-time", "half time", "ft", "ht",
        "kick-off", "kickoff", "line-up", "line-ups", "lineup", "lineups", "xi",
        "player ratings", "ratings", "match report", "as it happened", "live",
        "final score", "scoreline", "draw", "win", "won", "lose", "loss", "lost",
        "defeat", "beat", "goal", "goals", "penalty", "red card", "sent off",
        "preview", "watch:", "highlights", "vs", "v",
    ),
    scorelineRegex = Regex("""\b\d+\s*[-–—]\s*\d+\b"""),
    transferContextAllowlist = emptyList(),
    categoryKeywords = listOf(
        Category.TRANSFER to listOf(
            "transfer", "sign", "signing", "signed", "deal", "fee", "bid", "medical",
            "agree", "agreed", "loan", "target", "linked", "swoop", "here we go", "release clause",
        ),
        Category.COACHING to listOf(
            "appoint", "appointed", "appoints", "sacked", "new manager", "new coach",
            "assistant", "sporting director", "head coach",
        ),
        Category.CONTRACT to listOf("contract", "new deal", "extension", "renew", "terms"),
        Category.BACKROOM to listOf(
            "backroom", "head of fitness", "set-piece coach", "staff reshuffle",
            "sports science", "recruitment",
        ),
        Category.ACADEMY to listOf("academy", "u18", "u21", "u23", "youth", "graduate", "prospect"),
    ),
)
