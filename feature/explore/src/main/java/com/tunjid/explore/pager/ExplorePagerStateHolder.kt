package com.tunjid.explore.pager

import androidx.lifecycle.ViewModel
import com.tunjid.explore.pager.di.startingUrls
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@AssistedFactory
interface ExplorePagerStateHolderFactory : ScreenStateHolderCreator {
    override fun create(
        scope: CoroutineScope,
        route: Route,
    ): ExplorePagerViewModel
}

class ExplorePagerViewModel @AssistedInject constructor(
    playerManager: PlayerManager,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), ActionStateMutator<Action, StateFlow<State>> by scope.mutator(
    playerManager = playerManager,
    navigationActions = navigationActions,
    route = route
)

private fun CoroutineScope.mutator(
    playerManager: PlayerManager,
    navigationActions: (NavigationMutation) -> Unit,
    route: Route,
) = actionStateFlowMutator<Action, State>(
    initialState = State(
        playerManager = playerManager,
        items = route.preSeededNavigationItems(playerManager)
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Navigation -> action.flow
                    .map(::navigationEdits)
                    .consumeNavigationActions(
                        navigationActions
                    )
            }
        }
    }
)

private fun Route.preSeededNavigationItems(playerManager: PlayerManager) =
    routeParams.startingUrls.map { GalleryItem.Preview(playerManager.stateFor(it)) }
