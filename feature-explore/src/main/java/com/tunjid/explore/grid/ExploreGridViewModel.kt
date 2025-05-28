package com.tunjid.explore.grid

import androidx.lifecycle.ViewModel
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.di.AssistedViewModelFactory
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

typealias ExploreGridStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
interface ExploreGridModelFactory : AssistedViewModelFactory {
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
), ExploreGridStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        items = VideoUrls.map { url ->
            VideoItem(
                state = playerManager.stateFor(url = url),
            )
        }
    ),
    started = SharingStarted.WhileSubscribed(3000),

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

private fun Flow<Action.Play>.playMutations(
    playerManager: PlayerManager
): Flow<Mutation<State>> = mapLatestToManyMutations {
    playerManager.play(it.url)
}
