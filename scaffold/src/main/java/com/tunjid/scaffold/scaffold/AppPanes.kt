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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.tunjid.scaffold.adaptiveSpringSpec
import com.tunjid.scaffold.globalui.BackHandler
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.progress
import kotlin.math.max
import kotlin.math.min
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

    val currentPaneAnchor: PaneAnchor
        get() {
            val cappedFraction = max(
                a = min(
                    a = anchoredDraggableState.requireOffset() / maxWidth,
                    b = 1f
                ),
                b = 0f
            )
            return when (cappedFraction) {
                in 0f..0.01f -> PaneAnchor.Zero
                in Float.MIN_VALUE..PaneAnchor.OneThirds.fraction -> PaneAnchor.OneThirds
                in PaneAnchor.OneThirds.fraction..PaneAnchor.TwoThirds.fraction -> PaneAnchor.Half
                in PaneAnchor.TwoThirds.fraction..0.99f -> PaneAnchor.TwoThirds
                else -> PaneAnchor.Full
            }
        }

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
        PaneAnchor.entries.forEach { it at maxWidth * it.fraction }
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
