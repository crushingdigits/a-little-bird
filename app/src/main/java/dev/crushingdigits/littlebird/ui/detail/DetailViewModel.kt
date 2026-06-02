package dev.crushingdigits.littlebird.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItem
import dev.crushingdigits.littlebird.ui.navigation.DetailKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val key: DetailKey,
    observeNewsItem: ObserveNewsItem,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = observeNewsItem(key.itemId)
        .map { item ->
            if (item == null) DetailUiState(item = null, isLoading = false, notFound = true)
            else DetailUiState(item = item, isLoading = false, notFound = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    @AssistedFactory
    interface Factory { fun create(key: DetailKey): DetailViewModel }
}
