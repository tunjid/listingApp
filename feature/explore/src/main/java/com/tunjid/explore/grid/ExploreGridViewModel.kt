package com.tunjid.explore.grid

import androidx.lifecycle.ViewModel
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

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
        playerManager = playerManager,
    ),
    started = SharingStarted.WhileSubscribed(3000),
    inputs = listOf(),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Navigation -> action.flow
                    .consumeNavigationActions(
                        navigationActions
                    )
            }
        }
    }
)
