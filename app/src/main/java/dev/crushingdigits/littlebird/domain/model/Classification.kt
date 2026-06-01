package dev.crushingdigits.littlebird.domain.model

data class Classification(
    val isMatch: Boolean,
    val category: Category,
    val tier: Tier,
    val confirmed: Boolean,
)
