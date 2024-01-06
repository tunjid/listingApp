package com.tunjid.scaffold.globalui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.tunjid.mutator.mutation
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope

/**
 * Provides a way of composing the [UiState] on a global level.
 * This allows for coordination of the UI across navigation destinations.
 */
@Composable
fun ScreenUiState(state: UiState) {
    val scope = LocalAdaptiveContentScope.current ?: return
    val uiStateHolder = LocalGlobalUiStateHolder.current
    val updatedState by rememberUpdatedState(state)

    val fabClickListener = MutableFunction(state.fabClickListener)
    val snackbarMessageConsumer = MutableFunction(state.snackbarMessageConsumer)

    LaunchedEffect(updatedState, scope.containerState) {
        if (scope.containerState.container == Adaptive.Container.Primary) uiStateHolder.accept(
            mutation {
                // Preserve things that should not be overwritten
                updatedState.copy(
                    navMode = navMode,
                    windowSizeClass = windowSizeClass,
                    systemUI = systemUI,
                    backStatus = backStatus,
                    paneAnchor = paneAnchor,
                    fabClickListener = fabClickListener,
                    snackbarMessageConsumer = snackbarMessageConsumer,
                )
            })
    }

    DisposableEffect(true) {
        onDispose {
            fabClickListener.backing = {}
            snackbarMessageConsumer.backing = {}
        }
    }
}

/**
 * Generic function that helps override the backing implementation to prevent memory leaks
 */
private data class MutableFunction<T>(var backing: (T) -> Unit = {}) : (T) -> Unit {
    override fun invoke(item: T) = backing(item)
}