package dev.crushingdigits.littlebird.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Home scaffold (Feed + bottom nav). Fixed start destination, bottom of the back stack. */
@Serializable
data object FeedKey : NavKey

/** Full-screen reader for a single news item, identified by its stable id. */
@Serializable
data class DetailKey(val itemId: String) : NavKey
