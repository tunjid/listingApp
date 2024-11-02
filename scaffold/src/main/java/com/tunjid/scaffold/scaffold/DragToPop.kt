package com.tunjid.scaffold.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.dragtodismiss.dragToDismiss
import com.tunjid.scaffold.globalui.BackStatus
import com.tunjid.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route

@Composable
fun Modifier.dragToPop(): Modifier {
    val state = LocalDragToDismissState.current
    DisposableEffect(state) {
        state.enabled = true
        onDispose { state.enabled = false }
    }
    // TODO: This should not be necessary. Figure out why a frame renders with
    //  an offset of zero while the content in the transient primary container
    //  is still visible.
    val dragToDismissOffset by rememberUpdatedStateIf(
        value = state.offset.round(),
        predicate = {
            it != IntOffset.Zero
        }
    )
    return offset { dragToDismissOffset }
}

@Composable
internal fun PanedNavHostScope<ThreePane, Route>.DragToPopLayout(
    state: DragToDismissState,
    pane: ThreePane,
) {
    // Only place the DragToDismiss Modifier on the Primary pane
    if (pane == ThreePane.Primary) {
        Box(
            modifier = Modifier.dragToPopInternal(state)
        ) {
            Destination(pane)
        }
        // Place the transient primary screen above  the primary
        Destination(ThreePane.TransientPrimary)
    } else {
        Destination(pane)
    }
}

@Composable
private fun Modifier.dragToPopInternal(state: DragToDismissState): Modifier {
    val globalUiStateHolder = LocalGlobalUiStateHolder.current
    val navigationStateHolder = LocalNavigationStateHolder.current
    val density = LocalDensity.current
    val dismissThreshold = remember { with(density) { 200.dp.toPx().let { it * it } } }

    return dragToDismiss(
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
        onCancelled = {
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
            navigationStateHolder.accept {
                navState.pop()
            }
        }
    )
}

internal val LocalDragToDismissState = staticCompositionLocalOf {
    DragToDismissState()
}