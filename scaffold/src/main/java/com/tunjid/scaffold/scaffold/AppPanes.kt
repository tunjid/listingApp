package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.tunjid.scaffold.adaptiveSpringSpec
import com.tunjid.scaffold.globalui.BackHandler
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.progress
import kotlin.math.max
import kotlin.math.roundToInt

internal val LocalPaneAnchorState = staticCompositionLocalOf<PaneAnchorState> {
    TODO()
}

private val PaneSpring = adaptiveSpringSpec(
    visibilityThreshold = 0.1f
)

@Stable
internal class PaneAnchorState(density: Density) {
    var maxWidth by mutableIntStateOf(1000)
        internal set
    val width
        get() = max(
            a = 0,
            b = anchoredDraggableState.offset.roundToInt()
        )

    val targetPaneAnchor get() = anchoredDraggableState.targetValue

    val currentPaneAnchor get() = anchoredDraggableState.currentValue

    private val thumbMutableInteractionSource = MutableInteractionSource()

    val thumbInteractionSource: InteractionSource = thumbMutableInteractionSource

    private val anchoredDraggableState = AnchoredDraggableState(
        initialValue = PaneAnchor.OneThirds,
        anchors = currentAnchors(),
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { 100f },
        snapAnimationSpec = PaneSpring,
        decayAnimationSpec = splineBasedDecay(density)
    )

    val modifier = Modifier
        .hoverable(thumbMutableInteractionSource)
        .anchoredDraggable(
            state = anchoredDraggableState,
            orientation = Orientation.Horizontal,
            interactionSource = thumbMutableInteractionSource,
        )

    fun updateMaxWidth(maxWidth: Int) {
        this.maxWidth = maxWidth
        anchoredDraggableState.updateAnchors(currentAnchors())
    }

    fun dispatch(delta: Float) {
        anchoredDraggableState.dispatchRawDelta(delta)
    }

    suspend fun completeDispatch() = anchoredDraggableState.settle(velocity = 0f)

    suspend fun moveTo(anchor: PaneAnchor) = anchoredDraggableState.animateTo(
        targetValue = anchor,
    )

    private fun currentAnchors() = DraggableAnchors {
        PaneAnchor.Zero at 0f
        PaneAnchor.OneThirds at (maxWidth * (1f / 3))
        PaneAnchor.Half at (maxWidth * (1f / 2))
        PaneAnchor.TwoThirds at (maxWidth * (2f / 3))
        PaneAnchor.Full at maxWidth.toFloat()
    }
}

/**
 * Maps a back gesture to shutting the secondary pane
 */
@Composable
fun SecondaryPaneCloseBackHandler(enabled: Boolean) {
    val paneSplitState = LocalPaneAnchorState.current
    var started by remember { mutableStateOf(false) }
    var widthAtStart by remember { mutableIntStateOf(0) }
    var desiredPaneWidth by remember { mutableFloatStateOf(0f) }
    val animatedDesiredPanelWidth by animateFloatAsState(
        label = "DesiredAppPanelWidth",
        targetValue = desiredPaneWidth,
    )

    BackHandler(
        enabled = enabled,
        onStarted = {
            widthAtStart = paneSplitState.width
            started = true
        },
        onProgressed = { backStatus ->
            val backProgress = backStatus.progress
            val distanceToCover = paneSplitState.maxWidth - widthAtStart
            desiredPaneWidth = (backProgress * distanceToCover) + widthAtStart
        },
        onCancelled = {
            started = false
        },
        onBack = {
            started = false
        }
    )

    // Make sure desiredPaneWidth is synced with paneSplitState.width before the back gesture
    LaunchedEffect(started, paneSplitState.width) {
        if (started) return@LaunchedEffect
        desiredPaneWidth = paneSplitState.width.toFloat()
    }

    // Dispatch changes as the user presses back
    LaunchedEffect(started, animatedDesiredPanelWidth) {
        if (!started) return@LaunchedEffect
        paneSplitState.dispatch(delta = animatedDesiredPanelWidth - paneSplitState.width.toFloat())
    }

    // Fling to settle
    LaunchedEffect(started) {
        if (started) return@LaunchedEffect
        paneSplitState.completeDispatch()
    }
}
