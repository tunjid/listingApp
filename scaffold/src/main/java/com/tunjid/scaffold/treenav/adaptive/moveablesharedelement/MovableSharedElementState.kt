package com.tunjid.scaffold.treenav.adaptive.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastRoundToInt
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptivePaneState
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

@Stable
@OptIn(ExperimentalSharedTransitionApi::class)
internal class MovableSharedElementState<State, Pane, Destination : Node>(
    paneScope: AdaptivePaneScope<Pane, Destination>,
    sharedTransitionScope: SharedTransitionScope,
    sharedElement: @Composable (State, Modifier) -> Unit,
    onRemoved: () -> Unit,
    private val boundsTransform: BoundsTransform,
    private val canAnimateOnStartingFrames: AdaptivePaneState<Pane, Destination>.() -> Boolean
) : SharedElementOverlay, SharedTransitionScope by sharedTransitionScope {

    var paneScope by mutableStateOf(paneScope)

    private var inCount by mutableIntStateOf(0)

    private var layer: GraphicsLayer? = null
    private var targetOffset by mutableStateOf(IntOffset.Zero)
    private var boundsAnimInProgress by mutableStateOf(false)

    private val canDrawInOverlay get() = boundsAnimInProgress
    private val panesKeysToSeenCount = mutableStateMapOf<String, Unit>()

    private val animatedBounds: Rect?
        get() = if (boundsAnimInProgress) boundsAnimation.animatedValue else null

    val boundsAnimation = BoundsTransformDeferredAnimation()

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as State,
                Modifier
                    .movableSharedElement(
                        state = this,
                    ) then modifier,
            )

            DisposableEffect(Unit) {
                ++inCount
                onDispose {
                    if (--inCount <= 0) onRemoved()
                }
            }
        }

    override fun ContentDrawScope.drawInOverlay() {
        if (!canDrawInOverlay) return
        val overlayLayer = layer ?: return
        val (x, y) = targetOffset.toOffset()
        translate(x, y) {
            drawLayer(overlayLayer)
        }
    }

    private fun updatePaneStateSeen(
        paneState: AdaptivePaneState<*, *>
    ) {
        panesKeysToSeenCount[paneState.key] = Unit
    }

    private val hasBeenShared get() = panesKeysToSeenCount.size > 1

    companion object {
        /**
         * Allows a custom modifier to animate the local position and size of the layout within the
         * LookaheadLayout, whenever there's a change in the layout.
         */
        @OptIn(
            ExperimentalSharedTransitionApi::class
        )
        @Composable
        internal fun <Pane, Destination : Node> Modifier.movableSharedElement(
            state: MovableSharedElementState<*, Pane, Destination>,
        ): Modifier {
            val coroutineScope = rememberCoroutineScope()
            state.isInProgress().also { state.boundsAnimInProgress = it }
            val layer = rememberGraphicsLayer().also {
                state.layer = it
            }
            return approachLayout(
                isMeasurementApproachInProgress = { lookaheadSize ->
                    // Update target size, it will serve to know if we expect an approach in progress
                    state.boundsAnimation.updateTargetSize(lookaheadSize.toSize())
                    state.boundsAnimInProgress
                },
                isPlacementApproachInProgress = {
                    state.boundsAnimation.updateTargetOffsetAndAnimate(
                        lookaheadScope = state,
                        placementScope = this,
                        coroutineScope = coroutineScope,
                        includeMotionFrameOfReference = true,
                        boundsTransform = state.boundsTransform,
                    )
                    state.boundsAnimInProgress
                },
                approachMeasure = { measurable, _ ->
                    // The animated value is null on the first frame as we don't get the full bounds
                    // information until placement, so we can safely use the current Size.
                    val fallbackSize =
                        // When using Intrinsics, we may get measured before getting the approach check
                        if (state.boundsAnimation.currentSize.isUnspecified) lookaheadSize.toSize()
                        else state.boundsAnimation.currentSize

                    val (animatedWidth, animatedHeight) =
                        (state.animatedBounds?.size ?: fallbackSize).roundToIntSize()

                    // For the target Layout, pass the animated size as Constraints.
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = animatedWidth,
                            height = animatedHeight,
                        )
                    )
                    layout(animatedWidth, animatedHeight) {
                        val animatedBounds = state.animatedBounds
                        val currentCoordinates = coordinates ?: return@layout placeable.place(
                            x = 0,
                            y = 0
                        )
                        val positionInScope = with(state) {
                            lookaheadScopeCoordinates.localPositionOf(
                                sourceCoordinates = currentCoordinates,
                                relativeToSource = Offset.Zero,
                                includeMotionFrameOfReference = true,
                            )
                        }

                        val topLeft =
                            if (animatedBounds != null) {
                                state.boundsAnimation.updateCurrentBounds(
                                    animatedBounds.topLeft,
                                    animatedBounds.size
                                )
                                animatedBounds.topLeft
                            } else {
                                state.boundsAnimation.currentBounds?.topLeft ?: Offset.Zero
                            }
                        state.targetOffset = topLeft.round()

                        val (x, y) = topLeft - positionInScope
                        placeable.place(x.fastRoundToInt(), y.fastRoundToInt())
                    }
                }
            )
                .drawWithContent {
                    layer.record {
                        this@drawWithContent.drawContent()
                    }
                    if (!state.canDrawInOverlay) {
                        drawLayer(layer)
                    }
                }
        }


        @Composable
        private fun <Pane, Destination : Node> MovableSharedElementState<*, Pane, Destination>.isInProgress(): Boolean {
            val paneState = paneScope.paneState.also(::updatePaneStateSeen)

            val (laggingScopeKey, animationInProgressTillFirstIdle) = produceState(
                initialValue = Pair(
                    paneState.key,
                    paneState.canAnimateOnStartingFrames()
                ),
                key1 = paneState.key
            ) {
                value = Pair(
                    paneState.key,
                    paneState.canAnimateOnStartingFrames()
                )
                value = snapshotFlow { boundsAnimation.isIdle }
                    .debounce { if (it) 10 else 0 }
                    .first(true::equals)
                    .let { value.first to false }
            }.value


            if (!hasBeenShared) return false

            val isLagging = laggingScopeKey != paneScope.paneState.key
            val canAnimateOnStartingFrames = paneScope.paneState.canAnimateOnStartingFrames()

            if (isLagging) return canAnimateOnStartingFrames

            return animationInProgressTillFirstIdle
        }
    }
}

private val AdaptivePaneState<*, *>.key get() = "${currentDestination?.id}-$pane"
