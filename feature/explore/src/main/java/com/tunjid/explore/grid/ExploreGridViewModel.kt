package com.tunjid.explore.grid

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.media.PlayerManager
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.consumeNavigationActions
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.take

typealias ExploreGridStateHolder = ActionStateMutator<Action, StateFlow<State>>


@AssistedFactory
interface ExploreGridModelFactory : ScreenStateHolderCreator {
    override fun create(
        scope: CoroutineScope,
        route: Route,
    ): ExploreGridViewModel
}

class ExploreGridViewModel @AssistedInject constructor(
    playerManager: PlayerManager,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), ActionStateMutator<Action, StateFlow<State>> by scope.listingFeedStateHolder(
    playerManager = playerManager,
    navigationActions = navigationActions,
    route = route
)

fun CoroutineScope.listingFeedStateHolder(
    playerManager: PlayerManager,
    navigationActions: (NavigationMutation) -> Unit,
    route: Route,
): ExploreGridStateHolder = actionStateFlowMutator(
    initialState = State(
        items = VideoUrls.map { url ->
            VideoItem(
                state = playerManager.stateFor(url = url),
            )
        }
    ),
    started = SharingStarted.WhileSubscribed(3000),
    inputs = listOf(
        playerManager.playingUrlAtEntranceMutations(),
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Play -> action.flow.playMutations(playerManager)
                is Action.Navigation -> action.flow.consumeNavigationActions(
                    navigationActions
                )
            }
        }
    }
)

private fun PlayerManager.playingUrlAtEntranceMutations(): Flow<Mutation<State>> =
    snapshotFlow { currentUrl }
        // Only want to see the url playing at screen entrance
        .take(1)
        .mapToMutation { copy(playingUrlAtEntrance = it) }

private fun Flow<Action.Play>.playMutations(
    playerManager: PlayerManager
): Flow<Mutation<State>> = mapLatestToManyMutations {
    playerManager.play(it.url)
}