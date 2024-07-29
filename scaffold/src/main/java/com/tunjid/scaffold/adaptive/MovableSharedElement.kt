package com.tunjid.scaffold.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.tunjid.scaffold.adaptive.Adaptive.key
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

interface SharedElementOverlay {
    fun ContentDrawScope.drawInOverlay()
}

internal interface MovableSharedElementScope {
    fun isCurrentlyShared(key: Any): Boolean

    @Composable
    fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

fun thumbnailSharedElementKey(
    property: Any?
) = "thumbnail-$property"

@Stable
@OptIn(ExperimentalAnimatableApi::class)
internal class MovableSharedElementData<T>(
    sharedElement: @Composable (T, Modifier) -> Unit,
    onRemoved: () -> Unit
) : SharedElementOverlay {
    private var inCount by mutableIntStateOf(0)

    private var layer: GraphicsLayer? = null
    private var targetOffset by mutableStateOf(IntOffset.Zero)
    private var sizeAnimInProgress by mutableStateOf(false)
    private var offsetAnimInProgress by mutableStateOf(false)

    private val canDrawInOverlay get() = sizeAnimInProgress || offsetAnimInProgress
    private val panesKeysToSeenCount = mutableStateMapOf<String, Unit>()

    val offsetAnimation = DeferredTargetAnimation(
        vectorConverter = IntOffset.VectorConverter,
    )
    val sizeAnimation = DeferredTargetAnimation(
        vectorConverter = IntSize.VectorConverter,
    )

    val moveableSharedElement: @Composable (Any?, Modifier) -> Unit =
        movableContentOf { state, modifier ->
            @Suppress("UNCHECKED_CAST")
            sharedElement(
                // The shared element composable will be created by the first screen and reused by
                // subsequent screens. This updates the state from other screens so changes are seen.
                state as T,
                Modifier
                    .movableSharedElement(
                        movableSharedElementData = this,
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
        paneState: Adaptive.PaneState
    ) {
        panesKeysToSeenCount[paneState.key] = Unit
    }

    private fun hasBeenShared() = panesKeysToSeenCount.size > 1

    companion object {
        /**
         * Allows a custom modifier to animate the local position and size of the layout within the
         * LookaheadLayout, whenever there's a change in the layout.
         */
        @OptIn(
            ExperimentalAnimatableApi::class,
            ExperimentalSharedTransitionApi::class
        )
        internal fun Modifier.movableSharedElement(
            movableSharedElementData: MovableSharedElementData<*>,
        ): Modifier = composed {
            val sharedTransitionScope = LocalSharedTransitionScope.current
            val coroutineScope = rememberCoroutineScope()

            val sizeAnimInProgress = movableSharedElementData.isInProgress(
                MovableSharedElementData<*>::sizeAnimation
            )
                .also { movableSharedElementData.sizeAnimInProgress = it }

            val offsetAnimInProgress = movableSharedElementData.isInProgress(
                MovableSharedElementData<*>::offsetAnimation
            )
                .also { movableSharedElementData.offsetAnimInProgress = it }

            val layer = rememberGraphicsLayer().also {
                movableSharedElementData.layer = it
            }
            approachLayout(
                isMeasurementApproachInProgress = { lookaheadSize ->
                    movableSharedElementData.sizeAnimation.updateTarget(
                        target = lookaheadSize,
                        coroutineScope = coroutineScope,
                        animationSpec = sizeSpec
                    )
                    sizeAnimInProgress
                },
                isPlacementApproachInProgress = { lookaheadCoordinates ->
                    val lookaheadOffset = with(sharedTransitionScope) {
                        lookaheadScopeCoordinates.localLookaheadPositionOf(
                            sourceCoordinates = lookaheadCoordinates
                        ).round()
                    }
                    movableSharedElementData.offsetAnimation.updateTarget(
                        target = lookaheadOffset,
                        coroutineScope = coroutineScope,
                        animationSpec = offsetSpec,
                    )
                    offsetAnimInProgress
                },
                approachMeasure = { measurable, _ ->
                    val (width, height) = movableSharedElementData.sizeAnimation.updateTarget(
                        target = lookaheadSize,
                        coroutineScope = coroutineScope,
                        animationSpec = sizeSpec
                    )
                    val animatedConstraints = Constraints.fixed(width, height)
                    val placeable = measurable.measure(animatedConstraints)

                    layout(placeable.width, placeable.height) layout@{
                        val currentCoordinates = coordinates ?: return@layout placeable.place(
                            x = 0,
                            y = 0
                        )
                        val lookaheadOffset = with(sharedTransitionScope) {
                            lookaheadScopeCoordinates.localLookaheadPositionOf(
                                sourceCoordinates = currentCoordinates
                            ).round()
                        }

                        movableSharedElementData.apply {
                            targetOffset = offsetAnimation.updateTarget(
                                target = lookaheadOffset,
                                coroutineScope = coroutineScope,
                                animationSpec = offsetSpec
                            )
                        }

                        val (x, y) = movableSharedElementData.targetOffset - lookaheadOffset
                        placeable.place(
                            x = x,
                            y = y
                        )
                    }
                }
            )
                .drawWithContent {
                    layer.record {
                        this@drawWithContent.drawContent()
                    }
                    if (!movableSharedElementData.canDrawInOverlay) {
                        drawLayer(layer)
                    }
                }
        }


        @OptIn(ExperimentalAnimatableApi::class)
        @Composable
        private fun MovableSharedElementData<*>.isInProgress(
            animationMapper: (MovableSharedElementData<*>) -> DeferredTargetAnimation<*, *>
        ): Boolean {
            val animation = remember { animationMapper(this) }
            val paneState = LocalAdaptiveContentScope.current
                ?.paneState
                ?.also(::updatePaneStateSeen)

            val (laggingScopeKey, animationInProgressTillFirstIdle) = produceState(
                initialValue = Pair(
                    paneState?.key,
                    paneState.canAnimateOnStartingFrames()
                ),
                key1 = paneState
            ) {
                value = Pair(
                    paneState?.key,
                    paneState.canAnimateOnStartingFrames()
                )
                value = snapshotFlow { animation.isIdle }
                    .filter(true::equals)
                    .first()
                    .let { value.first to false }
            }.value

            return if (laggingScopeKey == paneState?.key) animationInProgressTillFirstIdle
                    && hasBeenShared()
            else paneState.canAnimateOnStartingFrames()
        }

        private fun Adaptive.PaneState?.canAnimateOnStartingFrames() =
            this?.pane != Adaptive.Pane.TransientPrimary

        private val sizeSpec = spring(
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = IntSize.VisibilityThreshold
        )

        private val offsetSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = 0.9f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    }
}

