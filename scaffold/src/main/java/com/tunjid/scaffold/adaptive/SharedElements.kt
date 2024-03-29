package com.tunjid.scaffold.adaptive

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

internal interface SharedElementScope {
    fun isCurrentlyShared(key: Any): Boolean

    @Composable
    fun <T> sharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

fun thumbnailSharedElementKey(
    property: Any?
) = "thumbnail-$property"

@Stable
@OptIn(ExperimentalAnimatableApi::class)
internal class SharedElementData<T>(
    sharedElement: @Composable (T, Modifier) -> Unit,
    onRemoved: () -> Unit
) {
    private var inCount by mutableIntStateOf(0)
    private var adaptiveContentScopesSeen by mutableIntStateOf(0)
    private var currentAdaptiveContentScopeKey by mutableStateOf<String?>(null)

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
                    .sharedElement(
                        sharedElementData = this,
                    ) then modifier,
            )

            DisposableEffect(Unit) {
                ++inCount
                onDispose {
                    if (--inCount <= 0) onRemoved()
                }
            }
        }

    internal fun canDeferToAnimationForCompletion(
        adaptiveContentScopeKey: String?
    ): Boolean {
        if (adaptiveContentScopeKey == null
            || adaptiveContentScopeKey == currentAdaptiveContentScopeKey
        ) return adaptiveContentScopesSeen > 1

        currentAdaptiveContentScopeKey = adaptiveContentScopeKey
        ++adaptiveContentScopesSeen
        return false
    }
}

/**
 * Allows a custom modifier to animate the local position and size of the layout within the
 * LookaheadLayout, whenever there's a change in the layout.
 */
@OptIn(ExperimentalAnimatableApi::class)
internal fun Modifier.sharedElement(
    sharedElementData: SharedElementData<*>,
): Modifier = this then composed {
    val lookaheadScope = LocalLookaheadScope.current
    val coroutineScope = rememberCoroutineScope()

    val sizeAnimComplete = sharedElementData.isComplete(
        SharedElementData<*>::sizeAnimation
    )
    val offsetAnimComplete = sharedElementData.isComplete(
        SharedElementData<*>::offsetAnimation
    )
    approachLayout(
        isMeasurementApproachComplete = {
            sharedElementData.sizeAnimation.updateTarget(
                target = it,
                coroutineScope = coroutineScope,
                animationSpec = sizeSpec
            )
            sizeAnimComplete
        },
        isPlacementApproachComplete = {
            val target = with(lookaheadScope) {
                lookaheadScopeCoordinates.localLookaheadPositionOf(it)
            }
            sharedElementData.offsetAnimation.updateTarget(
                target = target.round(),
                coroutineScope = coroutineScope,
                animationSpec = offsetSpec,
            )
            offsetAnimComplete
        },
        approachMeasure = { measurable, _ ->
            val (width, height) = sharedElementData.sizeAnimation.updateTarget(
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
                val targetOffset = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(
                        currentCoordinates
                    )
                }
                val animatedOffset = sharedElementData.offsetAnimation.updateTarget(
                    target = targetOffset.round(),
                    coroutineScope = coroutineScope,
                    animationSpec = offsetSpec
                )

                val currentOffset = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(
                        sourceCoordinates = currentCoordinates,
                        relativeToSource = Offset.Zero
                    ).round()
                }

                val (x, y) = animatedOffset - currentOffset
                placeable.place(
                    x = x,
                    y = y
                )
            }
        }
    )
}

@OptIn(ExperimentalAnimatableApi::class)
@Composable
private fun SharedElementData<*>.isComplete(
    animationMapper: (SharedElementData<*>) -> DeferredTargetAnimation<*, *>
): Boolean {
    val animation = remember { animationMapper(this) }
    val scopeKey = LocalAdaptiveContentScope.current?.key

    val animationCompleteAtLeastOnce by produceState(
        initialValue = false,
        key1 = scopeKey
    ) {
        value = false
        value = snapshotFlow { animation.isIdle }
            .filter(true::equals)
            .first()
    }

    return canDeferToAnimationForCompletion(scopeKey) && animationCompleteAtLeastOnce
}

private val sizeSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntSize.VisibilityThreshold
)

private val offsetSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = 0.9f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)