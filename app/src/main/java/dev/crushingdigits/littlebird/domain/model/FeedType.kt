package dev.crushingdigits.littlebird.domain.model

/** A single-select news-type feed. [categories] = null means "every category" (the All feed). */
enum class FeedType(val key: String, val categories: Set<Category>?) {
    ALL("all", null),
    TRANSFERS("transfer", setOf(Category.TRANSFER)),
    MATCH("match", setOf(Category.MATCH)),
    SQUAD_STAFF("staff", setOf(Category.COACHING, Category.BACKROOM)),
    CONTRACTS("contract", setOf(Category.CONTRACT)),
    ACADEMY("academy", setOf(Category.ACADEMY));

    fun matches(category: Category): Boolean = categories == null || category in categories

    companion object {
        fun fromKey(key: String?): FeedType = entries.find { it.key == key } ?: ALL
    }
}
