package com.tunjid.feature.listinggallery.pager

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.round
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.dragToDismiss(
    onDismissed: () -> Unit,
    animationSpec: AnimationSpec<Offset> = spring(),
    shouldDismiss: (Offset, Velocity) -> Boolean = { offset, _ ->
        offset.getDistanceSquared() > (500 * 500)
    }
): Modifier = composed {
    var offset by remember {
        mutableStateOf(Offset.Zero)
    }
    val draggable2DState = remember {
        Draggable2DState { dragAmount ->
            offset += dragAmount
        }
    }
    val scope = rememberCoroutineScope()
    var resetJob: Job? by remember {
        mutableStateOf(null)
    }
    offset { offset.round() }
        .draggable2D(
            state = draggable2DState,
            startDragImmediately = false,
            onDragStopped = { velocity ->
                if (shouldDismiss(offset, velocity)) onDismissed()
                else resetJob = scope.launch {
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
                    }
                }
            }
        )
}
