package com.tunjid.scaffold.treenav.adaptive.moveablesharedelement


import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
internal class BoundsTransformDeferredAnimation {
    private var animatable: Animatable<Rect, AnimationVector4D>? = null

    private var targetSize: Size = Size.Unspecified
    private var targetOffset: Offset = Offset.Unspecified

    private var isPending = false

    /**
     * Captures lookahead size, updates current size for the first pass and marks the animation as
     * pending.
     */
    fun updateTargetSize(size: Size) {
        if (targetSize.isSpecified && size.roundToIntSize() != targetSize.roundToIntSize()) {
            // Change in target, animation is pending
            isPending = true
        }
        targetSize = size

        if (currentSize.isUnspecified) {
            currentSize = size
        }
    }

    /**
     * Captures lookahead position, updates current position for the first pass and marks the
     * animation as pending.
     */
    private fun updateTargetOffset(offset: Offset) {
        if (targetOffset.isSpecified && offset.round() != targetOffset.round()) {
            isPending = true
        }
        targetOffset = offset

        if (currentPosition.isUnspecified) {
            currentPosition = offset
        }
    }

    // We capture the current bounds parameters individually to avoid unnecessary Rect allocations
    private var currentPosition: Offset = Offset.Unspecified
    var currentSize: Size = Size.Unspecified

    val currentBounds: Rect?
        get() {
            val size = currentSize
            val position = currentPosition
            return if (position.isSpecified && size.isSpecified) {
                Rect(position, size)
            } else {
                null
            }
        }

    fun updateCurrentBounds(position: Offset, size: Size) {
        currentPosition = position
        currentSize = size
    }

    val isIdle: Boolean
        get() = !isPending && animatable?.isRunning != true

    var animatedValue: Rect? by mutableStateOf(null)
        private set

//    val value: Rect?
//        get() = if (isIdle) null else animatedValue

    private var directManipulationParents: MutableList<LayoutCoordinates>? = null
    private var additionalOffset: Offset = Offset.Zero

    fun updateTargetOffsetAndAnimate(
        lookaheadScope: LookaheadScope,
        placementScope: Placeable.PlacementScope,
        coroutineScope: CoroutineScope,
        includeMotionFrameOfReference: Boolean,
        boundsTransform: BoundsTransform,
    ) {
        placementScope.coordinates?.let { coordinates ->
            with(lookaheadScope) {
                val lookaheadScopeCoordinates = placementScope.lookaheadScopeCoordinates

                var delta = Offset.Zero
                if (!includeMotionFrameOfReference) {
                    // As the Layout changes, we need to keep track of the accumulated offset up
                    // the hierarchy tree, to get the proper Offset accounting for scrolling.
                    val parents = directManipulationParents ?: mutableListOf()
                    var currentCoords = coordinates
                    var index = 0

                    // Find the given lookahead coordinates by traversing up the tree
                    while (currentCoords.toLookaheadCoordinates() != lookaheadScopeCoordinates) {
                        if (currentCoords.introducesMotionFrameOfReference) {
                            if (parents.size == index) {
                                parents.add(currentCoords)
                                delta += currentCoords.positionInParent()
                            } else if (parents[index] != currentCoords) {
                                delta -= parents[index].positionInParent()
                                parents[index] = currentCoords
                                delta += currentCoords.positionInParent()
                            }
                            index++
                        }
                        currentCoords = currentCoords.parentCoordinates ?: break
                    }

                    for (i in parents.size - 1 downTo index) {
                        delta -= parents[i].positionInParent()
                        parents.removeAt(parents.size - 1)
                    }
                    directManipulationParents = parents
                }
                additionalOffset += delta

                val targetOffset =
                    lookaheadScopeCoordinates.localLookaheadPositionOf(
                        sourceCoordinates = coordinates,
                        includeMotionFrameOfReference = includeMotionFrameOfReference
                    )
                updateTargetOffset(targetOffset + additionalOffset)

                animatedValue =
                    animate(coroutineScope = coroutineScope, boundsTransform = boundsTransform)
                        .translate(-(additionalOffset))
            }
        }
    }

    private fun animate(
        coroutineScope: CoroutineScope,
        boundsTransform: BoundsTransform,
    ): Rect {
        if (targetOffset.isSpecified && targetSize.isSpecified) {
            // Initialize Animatable when possible, we might not use it but we need to have it
            // instantiated since at the first pass the lookahead information will become the
            // initial bounds when we actually need an animation.
            val target = Rect(targetOffset, targetSize)
            val anim = animatable ?: Animatable(target, Rect.VectorConverter)
            animatable = anim

            // This check should avoid triggering an animation on the first pass, as there would not
            // be enough information to have a distinct current and target bounds.
            if (isPending) {
                isPending = false
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Dispatch right away to make sure approach callbacks are accurate on `isIdle`
                    anim.animateTo(target, boundsTransform.transform(currentBounds!!, target))
                }
            }
        }
        return animatable?.value ?: Rect.Zero
    }
}
