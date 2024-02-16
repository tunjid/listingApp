package com.tunjid.scaffold.globalui

import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.asNoOpStateFlowMutator
import com.tunjid.mutator.mutationOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

typealias GlobalUiStateHolder = ActionStateMutator<@JvmSuppressWildcards Mutation<UiState>, @JvmSuppressWildcards StateFlow<UiState>>

@Singleton
class ActualGlobalUiStateHolder @Inject constructor(
    appScope: CoroutineScope,
) : GlobalUiStateHolder by appScope.actionStateFlowMutator(
    initialState = UiState(),
    actionTransform = { it }
)

internal val LocalGlobalUiStateHolder = staticCompositionLocalOf {
    UiState().asNoOpStateFlowMutator<Mutation<UiState>, UiState>()
}

fun <State : Any> StateFlow<UiState>.navBarSizeMutations(
    mutation: State.(navbarSize: Int) -> State
): Flow<Mutation<State>> =
    map { it.navBarSize }
        .distinctUntilChanged()
        .map { mutationOf { mutation(this, it) } }