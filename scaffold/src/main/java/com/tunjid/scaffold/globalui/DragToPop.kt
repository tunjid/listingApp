package com.tunjid.scaffold.globalui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.treenav.pop
import kotlinx.coroutines.launch

@Stable
internal class DragToPopState {
    var enabled by mutableStateOf(false)
    var scale by mutableFloatStateOf(1f)
    var offset = mutableStateOf(Offset.Zero)
    var animationSpec by mutableStateOf(spring<Offset>())
    val clipRadius by derivedStateOf { 16.dp * (1f - scale) }
}

fun Modifier.dragToPop(): Modifier = composed {
    val state = LocalDragToPopState.current
    DisposableEffect(state) {
        state.enabled = true
        onDispose { state.enabled = false }
    }
    this
}

internal fun Modifier.dragToPopInternal(state: DragToPopState): Modifier = composed {
    val globalUiStateHolder = LocalGlobalUiStateHolder.current
    val navigationStateHolder = LocalNavigationStateHolder.current

    val animationSpec = state.animationSpec
    var offset by state.offset
    val draggable2DState = remember {
        Draggable2DState { dragAmount ->
            offset += dragAmount
        }
    }
    var started by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val scaleAnimatable = remember {
        Animatable(initialValue = state.scale)
    }
    LaunchedEffect(started) {
        scaleAnimatable.animateTo(
            animationSpec = spring(
                stiffness = Spring.StiffnessLow
            ),
            targetValue = if (started) 0.8f else 1f
        ) {
            state.scale = value
        }
    }

    draggable2D(
        state = draggable2DState,
        startDragImmediately = false,
        enabled = state.enabled,
        onDragStarted = {
            started = true
            globalUiStateHolder.accept {
                copy(backStatus = BackStatus.DragDismiss)
            }
        },
        onDragStopped = { velocity ->
            when {
                offset.getDistanceSquared() > (500 * 500) -> {
                    started = false
                    // Dismiss back preview
                    globalUiStateHolder.accept {
                        copy(backStatus = BackStatus.None)
                    }
                    // Pop navigation
                    navigationStateHolder.accept { navState.pop() }
                    offset = Offset.Zero
                }

                else -> scope.launch {
                    started = false
                    draggable2DState.drag {
                        animate(
                            typeConverter = Offset.VectorConverter,
                            initialValue = offset,
                            targetValue = Offset.Zero,
                            initialVelocity = Offset(
                                x = velocity.x,
                                y = velocity.y
                            ),
                            animationSpec = animationSpec,
                            block = { value, _ ->
                                dragBy(value - offset)
                            }
                        )
                        // Dismiss back preview
                        globalUiStateHolder.accept {
                            copy(backStatus = BackStatus.None)
                        }
                    }
                }
            }
        }
    )
}

internal val LocalDragToPopState = staticCompositionLocalOf {
    DragToPopState()
}