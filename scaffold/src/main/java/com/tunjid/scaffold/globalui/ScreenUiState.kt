package com.tunjid.scaffold.globalui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.tunjid.mutator.mutationOf
import com.tunjid.scaffold.scaffold.LocalAppState
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.threepane.ThreePane

/**
 * Provides a way of composing the [UiState] on a global level.
 * This allows for coordination of the UI across navigation destinations.
 */
@Composable
fun PaneScope<ThreePane, *>.ScreenUiState(state: UiState) {
    val appState = LocalAppState.current
    val updatedState by rememberUpdatedState(state)

    val fabClickListener = MutableFunction(state.fabClickListener)
    val snackbarMessageConsumer = MutableFunction(state.snackbarMessageConsumer)

    LaunchedEffect(updatedState, paneState) {
        if (paneState.pane == ThreePane.Primary && isActive) appState.updateGlobalUi(
            mutationOf {
                // Preserve things that should not be overwritten
                updatedState.copy(
                    navMode = navMode,
                    windowSizeClass = windowSizeClass,
                    systemUI = systemUI,
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