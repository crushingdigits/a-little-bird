package dev.crushingdigits.littlebird.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.domain.usecase.MarkWatched
import dev.crushingdigits.littlebird.domain.usecase.MarkWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtectionUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealed
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.RevealItemUseCase
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
    @Binds abstract fun markWatched(impl: MarkWatchedUseCase): MarkWatched
    @Binds abstract fun restoreRevealed(impl: RestoreRevealedUseCase): RestoreRevealed
}
