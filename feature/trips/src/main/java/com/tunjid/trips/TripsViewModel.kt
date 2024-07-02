package com.tunjid.trips

import androidx.lifecycle.ViewModel
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

typealias TripStateHolder = ActionStateMutator<Action, StateFlow<State>>


@AssistedFactory
interface TripsViewModelFactory : ScreenStateHolderCreator {
    override fun create(
        scope: CoroutineScope,
        route: Route,
    ): TripsViewModel
}

class TripsViewModel @AssistedInject constructor(
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), ActionStateMutator<Action, StateFlow<State>> by scope.listingFeedStateHolder(
    navigationActions = navigationActions,
    route = route
)

fun CoroutineScope.listingFeedStateHolder(
    navigationActions: (NavigationMutation) -> Unit,
    route: Route,
): TripStateHolder = actionStateFlowMutator(
    initialState = State(),
    started = SharingStarted.WhileSubscribed(3000),
    inputs = listOf(
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                else -> emptyFlow()
            }
        }
    }
)

