package dev.crushingdigits.littlebird.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.crushingdigits.littlebird.ui.components.BottomNav
import dev.crushingdigits.littlebird.ui.detail.DetailScreen
import dev.crushingdigits.littlebird.ui.detail.DetailViewModel
import dev.crushingdigits.littlebird.ui.feed.FeedScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(FeedKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<FeedKey> {
                HomeScaffold(onOpenDetail = { id -> backStack.add(DetailKey(id)) })
            }
            entry<DetailKey> { key ->
                val vm = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                DetailScreen(viewModel = vm, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

@Composable
private fun HomeScaffold(onOpenDetail: (String) -> Unit) {
    var tab by remember { mutableStateOf("feed") }
    Scaffold(bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) }) { padding ->
        when (tab) {
            "feed" -> FeedScreen(modifier = Modifier.padding(padding), onNavigateToDetail = onOpenDetail)
            else -> SettingsPlaceholder(Modifier.padding(padding))
        }
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings — coming soon", style = MaterialTheme.typography.titleLarge)
    }
}
