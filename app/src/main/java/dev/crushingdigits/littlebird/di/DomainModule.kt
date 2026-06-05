package dev.crushingdigits.littlebird.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWall
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallUseCase
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatched
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAt
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAtUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItem
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItemUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtectionUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveSchedule
import dev.crushingdigits.littlebird.domain.usecase.ObserveScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealed
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.RevealItemUseCase
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSync
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
import dev.crushingdigits.littlebird.domain.usecase.SyncSchedule
import dev.crushingdigits.littlebird.domain.usecase.SyncScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtection
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtectionUseCase

/** Binds each use-case action interface to its @Inject implementation, so consumers (the
 *  FeedViewModel) inject the abstraction rather than the concrete UseCase class. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds abstract fun observeFeed(impl: ObserveFeedUseCase): ObserveFeed
    @Binds abstract fun refreshFeed(impl: RefreshFeedUseCase): RefreshFeed
    @Binds abstract fun observeProtection(impl: ObserveProtectionUseCase): ObserveProtection
    @Binds abstract fun observeRevealed(impl: ObserveRevealedUseCase): ObserveRevealed
    @Binds abstract fun toggleProtection(impl: ToggleProtectionUseCase): ToggleProtection
    @Binds abstract fun revealItem(impl: RevealItemUseCase): RevealItem
    @Binds abstract fun restoreRevealed(impl: RestoreRevealedUseCase): RestoreRevealed
    @Binds abstract fun observeNewsItem(impl: ObserveNewsItemUseCase): ObserveNewsItem
    @Binds internal abstract fun filterFeed(impl: FilterFeedUseCase): FilterFeed
    @Binds internal abstract fun observeFilterPrefs(impl: ObserveFilterPrefsUseCase): ObserveFilterPrefs
    @Binds internal abstract fun setReliabilityLevel(impl: SetReliabilityLevelUseCase): SetReliabilityLevel
    @Binds internal abstract fun setPinnedFeed(impl: SetPinnedFeedUseCase): SetPinnedFeed
    @Binds abstract fun observeSchedule(impl: ObserveScheduleUseCase): ObserveSchedule
    @Binds abstract fun observeWatchedGames(impl: ObserveWatchedGamesUseCase): ObserveWatchedGames
    @Binds abstract fun observeLastSyncAt(impl: ObserveLastSyncAtUseCase): ObserveLastSyncAt
    @Binds abstract fun markGameWatched(impl: MarkGameWatchedUseCase): MarkGameWatched
    @Binds abstract fun restoreWatchedGames(impl: RestoreWatchedGamesUseCase): RestoreWatchedGames
    @Binds abstract fun computeSpoilerWall(impl: ComputeSpoilerWallUseCase): ComputeSpoilerWall
    @Binds abstract fun seedWatchedOnFirstSync(impl: SeedWatchedOnFirstSyncUseCase): SeedWatchedOnFirstSync
    @Binds abstract fun syncSchedule(impl: SyncScheduleUseCase): SyncSchedule
}
