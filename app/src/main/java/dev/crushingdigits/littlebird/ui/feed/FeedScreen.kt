package dev.crushingdigits.littlebird.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.ui.components.AppToast
import dev.crushingdigits.littlebird.ui.components.CatchUpCard
import dev.crushingdigits.littlebird.ui.components.CatchUpSheet
import dev.crushingdigits.littlebird.ui.components.GateSheet
import dev.crushingdigits.littlebird.ui.components.NewsCard
import dev.crushingdigits.littlebird.ui.components.ProtectionBanner
import dev.crushingdigits.littlebird.ui.components.RedactedCard
import dev.crushingdigits.littlebird.ui.components.TopBar
import dev.crushingdigits.littlebird.ui.feed.filter.FeedTypeTabs
import dev.crushingdigits.littlebird.ui.feed.filter.FilterEmpty
import dev.crushingdigits.littlebird.ui.feed.filter.PinButton
import dev.crushingdigits.littlebird.ui.feed.filter.ReliabilityLadder
import dev.crushingdigits.littlebird.ui.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onRefresh()
        viewModel.actions.collect { action ->
            when (action) {
                is FeedAction.ShowToast -> {
                    val result = snackbarHostState.showSnackbar(
                        message = action.message,
                        actionLabel = action.undoSnapshot?.let { "UNDO" },
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed && action.undoSnapshot != null) {
                        viewModel.onUndo(action.undoSnapshot)
                    }
                }
                is FeedAction.NavigateToDetail -> onNavigateToDetail(action.itemId)
            }
        }
    }

    FeedContent(state, snackbarHostState, viewModel, modifier)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FeedContent(
    state: FeedUiState,
    snackbarHostState: SnackbarHostState,
    vm: FeedViewModel,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = { TopBar(protectionOn = state.protectionOn, onToggleShield = vm::onToggleProtection) },
        snackbarHost = { AppToast(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FeedTypeTabs(
                tabs = state.filterTabs,
                selected = state.selectedType,
                pinned = state.pinnedType,
                onSelect = vm::onSelectType,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReliabilityLadder(selected = state.reliabilityLevel, onSelect = vm::onSetReliability)
                Spacer(Modifier.weight(1f))
                PinButton(pinned = state.selectedType == state.pinnedType, onToggle = vm::onTogglePin)
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::onRefresh,
                state = pullState,
                modifier = Modifier.weight(1f),
            ) {
                when {
                    state.isEmpty && state.isOffline -> EmptyState(
                        title = "You're offline",
                        subtitle = "Connect to load the latest LFC news.",
                    )
                    state.isEmpty -> EmptyState(
                        title = "Nothing yet",
                        subtitle = "Pull to refresh for the latest transfer & backroom news.",
                    )
                    state.isFilteredEmpty -> FilterEmpty(onShowAllTiers = vm::onResetReliability)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(14.dp, 0.dp, 14.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ProtectionBanner(
                                on = state.protectionOn,
                                onToggle = vm::onToggleProtection,
                            )
                        }
                        if (state.protectionOn && state.heldMatchCount > 0) {
                            item {
                                CatchUpCard(
                                    count = state.heldMatchCount,
                                    onMarkWatched = vm::onRequestCatchUp,
                                )
                            }
                        }
                        items(state.cards, key = { it.item.id }) { card ->
                            when (card) {
                                is FeedCard.Normal -> NewsCard(
                                    item = card.item,
                                    relativeTime = card.item.relativeTime(),
                                    onClick = { vm.onCardClick(card) },
                                )
                                is FeedCard.Redacted -> RedactedCard(
                                    item = card.item,
                                    onClick = { vm.onCardClick(card) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.gateTarget?.let { target ->
        GateSheet(
            source = target.source,
            onReveal = { vm.onRevealFromGate(target.id) },
            onKeepHidden = vm::onDismissGate,
            onDismiss = vm::onDismissGate,
        )
    }
    if (state.catchUpVisible) {
        CatchUpSheet(
            count = state.heldMatchCount,
            onConfirm = vm::onMarkWatched,
            onNotYet = vm::onDismissCatchUp,
            onDismiss = vm::onDismissCatchUp,
        )
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

