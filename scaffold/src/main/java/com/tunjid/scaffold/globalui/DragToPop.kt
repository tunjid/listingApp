package com.tunjid.scaffold.globalui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.treenav.pop

@Composable
fun Modifier.dragToPop(): Modifier {
    val state = LocalDragToDismissState.current
    DisposableEffect(state) {
        state.enabled = true
        onDispose { state.enabled = false }
    }
    return this
}

@Composable
internal fun Modifier.dragToPopInternal(state: DragToDismissState): Modifier {
    val globalUiStateHolder = LocalGlobalUiStateHolder.current
    val navigationStateHolder = LocalNavigationStateHolder.current
    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 200.dp.toPx().let { it * it } } }

    return this then Modifier.dragToDismiss(
        state = state,
        dragThresholdCheck = { offset, _ ->
            offset.getDistanceSquared() > dismissThreshold
        },
        // Enable back preview
        onStart = {
            globalUiStateHolder.accept {
                copy(backStatus = BackStatus.DragDismiss)
            }
        },
        onReset = {
            // Dismiss back preview
            globalUiStateHolder.accept {
                copy(backStatus = BackStatus.None)
            }
        },
        onDismissed = {
            // Dismiss back preview
            globalUiStateHolder.accept {
                copy(backStatus = BackStatus.None)
            }
            // Pop navigation
            navigationStateHolder.accept { navState.pop() }
        }
    )
}

internal val LocalDragToDismissState = staticCompositionLocalOf {
    DragToDismissState()
}