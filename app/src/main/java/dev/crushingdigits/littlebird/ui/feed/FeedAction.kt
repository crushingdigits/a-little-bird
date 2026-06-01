package dev.crushingdigits.littlebird.ui.feed

/** Fire-and-forget effects emitted by the ViewModel via SharedFlow (must not re-trigger on rotation). */
sealed interface FeedAction {
    /** Show a snackbar. If [undoSnapshot] is non-null, render an Undo action restoring that revealed set. */
    data class ShowToast(val message: String, val undoSnapshot: Set<String>?) : FeedAction
}
