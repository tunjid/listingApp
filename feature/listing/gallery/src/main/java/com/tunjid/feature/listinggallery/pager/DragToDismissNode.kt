package com.tunjid.feature.listinggallery.pager

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.mutationOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

fun Modifier.dragToDismiss(
    onEnded: () -> Unit,
    onCancelled: () -> Unit,
) = this then DragToDismissNodeElement(
    onEnded = onEnded,
    onCancelled = onCancelled
)

private data class DragToDismissNodeElement(
    val onEnded: () -> Unit,
    val onCancelled: () -> Unit,
) : ModifierNodeElement<DragToDismissNode>() {
    override fun create() = DragToDismissNode(
        onEnded = onEnded,
        onCancelled = onCancelled
    )

    override fun update(node: DragToDismissNode) {
        node.onEnded = onEnded
        node.onCancelled = onCancelled
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "DragToDismissNode"
        properties["onEnded"] = onEnded
        properties["onCancelled"] = onCancelled
    }
}

private class DragToDismissNode(
    var onEnded: () -> Unit,
    var onCancelled: () -> Unit,
) : DelegatingNode(),
    PointerInputModifierNode,
    LayoutModifierNode {

    private val offsetMutator by lazy {
        coroutineScope.offsetMutator()
    }

    private var offset by mutableStateOf(Offset.Zero)

    private val pointerInputDelegateNode = delegate(
        SuspendingPointerInputModifierNode {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetMutator.accept(
                        Gesture.Drag(Offset(x = dragAmount.x, y = dragAmount.y))
                    )
                },
                onDragEnd = {
                    if (offset.getDistanceSquared() > 300 * 300) onEnded()
                    else offsetMutator.accept(Gesture.Release)
                },
                onDragCancel = {
                    if (offset.getDistanceSquared() > 300 * 300) onCancelled()
                    else offsetMutator.accept(Gesture.Release)
                }
            )
        }
    )

    override fun onAttach() {
        coroutineScope.launch {
            offsetMutator.state.collect {
                offset = it
            }
        }
    }
    override fun onCancelPointerInput() =
        pointerInputDelegateNode.onCancelPointerInput()

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = pointerInputDelegateNode.onPointerEvent(
        pointerEvent = pointerEvent,
        pass = pass,
        bounds = bounds
    )

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val offsetValue = offset.round()
            placeable.placeWithLayer(offsetValue.x, offsetValue.y)
        }
    }
}

private sealed class Gesture {
    data class Drag(val offset: Offset) : Gesture()
    data object Release : Gesture()
}

private fun CoroutineScope.offsetMutator() = actionStateFlowMutator<Gesture, Offset>(
    initialState = Offset.Zero,
    actionTransform = { actions ->
        actions.flatMapLatest { action ->
            when (action) {
                is Gesture.Drag -> flowOf(
                    mutationOf {
                        copy(x = x + action.offset.x, y = y + action.offset.y)
                    }
                )

                Gesture.Release -> {
                    val (currentX, currentY) = state()
                    merge(
                        currentX.animate(to = 0f).mapToMutation { copy(x = it) },
                        currentY.animate(to = 0f).mapToMutation { copy(y = it) }
                    )
                }
            }
        }
    }
)

private fun Float.animate(to: Float) = callbackFlow {
    Animatable(initialValue = this@animate)
        .animateTo(to) {
            channel.trySend(value)
        }
    close()
}