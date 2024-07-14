package com.tunjid.explore.pager

import androidx.lifecycle.ViewModel
import com.tunjid.explore.pager.di.initialPage
import com.tunjid.explore.pager.di.startingUrls
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
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
        initialPage = route.routeParams.initialPage,
        items = route.preSeededNavigationItems(playerManager)
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Play -> action.flow.playMutations(playerManager)
                is Action.Navigation -> action.flow
                    .map(::navigationEdits)
                    .consumeNavigationActions(
                        navigationActions
                    )
            }
        }
    }
)

private fun Flow<Action.Play>.playMutations(
    playerManager: PlayerManager
): Flow<Mutation<State>> = mapLatestToManyMutations {
    playerManager.play(it.url)
}

private fun Route.preSeededNavigationItems(playerManager: PlayerManager) =
    routeParams.startingUrls.map { GalleryItem.Preview(playerManager.stateFor(it)) }
