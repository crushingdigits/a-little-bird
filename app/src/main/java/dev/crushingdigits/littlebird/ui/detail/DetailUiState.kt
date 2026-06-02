package dev.crushingdigits.littlebird.ui.detail

import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Immutable snapshot of the Detail reader. */
data class DetailUiState(
    val item: NewsItem? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
)
