package com.tunjid.scaffold.treenav.adaptive.moveablesharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptivePaneState

internal interface SharedElementOverlay {
    fun ContentDrawScope.drawInOverlay()
}

/**
 * Creates movable shared elements that may be shared amongst different [AdaptivePaneScope]
 * instances.
 */
interface MovableSharedElementScope {

    /**
     * Creates a movable shared element that accepts a single argument [T] and a [Modifier].
     *
     * NOTE: It is an error to compose the movable shared element in different locations
     * simultaneously, and the behavior of the shared element is undefined in this case.
     *
     * @param key the shared element key to identify the movable shared element.
     * @param sharedElement a factory function to create the shared element if it does not
     * currently exist.
     */
    fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform = DefaultBoundsTransform,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

/**
 * State for managing movable shared elements within a single [AdaptiveNavHost].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class MovableSharedElementHostState<Pane, Destination : Node>(
    private val sharedTransitionScope: SharedTransitionScope,
    private val canAnimateOnStartingFrames: (AdaptivePaneState<Pane, Destination>) -> Boolean,
) {

    // TODO: This should be unnecessary. Figure out a way to participate arbitrarily in the
    //  overlays already implemented in [SharedTransitionScope].
    /**
     * A [Modifier] for drawing the movable shared element in overlays over existing content.
     */
    val modifier = Modifier.drawWithContent {
        drawContent()
        overlays.forEach { overlay ->
            with(overlay) {
                drawInOverlay()
            }
        }
    }

    private val overlays: Collection<SharedElementOverlay>
        get() = keysToMovableSharedElements.values

    private val keysToMovableSharedElements =
        mutableStateMapOf<Any, MovableSharedElementState<*, Pane, Destination>>()

    /**
     * Returns true is a given shared element under a given key is currently being shared.
     */
    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    /**
     * Provides a movable shared element that can be rendered in a given [AdaptivePaneScope].
     * It is the callers responsibility to perform other verifications on the ability
     * of the calling [AdaptivePaneScope] to render the movable shared element.
     */
    fun <S> AdaptivePaneScope<Pane, Destination>.createOrUpdateSharedElement(
        key: Any,
        boundsTransform: BoundsTransform,
        sharedElement: @Composable (S, Modifier) -> Unit,
    ): @Composable (S, Modifier) -> Unit {
        val movableSharedElementState = keysToMovableSharedElements.getOrPut(key) {
            MovableSharedElementState(
                paneScope = this,
                sharedTransitionScope = sharedTransitionScope,
                sharedElement = sharedElement,
                boundsTransform = boundsTransform,
                canAnimateOnStartingFrames = canAnimateOnStartingFrames,
                onRemoved = { keysToMovableSharedElements.remove(key) }
            )
        }.also { it.paneScope = this }

        // Can't really guarantee that the caller will use the same key for the right type
        return movableSharedElementState.moveableSharedElement
    }
}

/**
 * An implementation of [MovableSharedElementScope] that ensures shared elements are only rendered
 * in an [AdaptivePaneScope] when it is active.
 *
 * Other implementations of [MovableSharedElementScope] may delegate to this for their own
 * movable shared element implementations.
 */
@Stable
internal class AdaptiveMovableSharedElementScope<T, R : Node>(
    paneScope: AdaptivePaneScope<T, R>,
    private val movableSharedElementHostState: MovableSharedElementHostState<T, R>,
) : MovableSharedElementScope {

    var paneScope by mutableStateOf(paneScope)

    override fun <T> movableSharedElementOf(
        key: Any,
        boundsTransform: BoundsTransform,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        // This pane state may be animating out. Look up the actual current route
        // Do not use the shared element if this content is being animated out
        if (!paneScope.isActive) return emptyComposable()

        return with(movableSharedElementHostState) {
            paneScope.createOrUpdateSharedElement(
                key = key,
                boundsTransform = boundsTransform,
                sharedElement = sharedElement
            )
        }
    }
}

private fun <T> emptyComposable(): @Composable (T, Modifier) -> Unit = EMPTY_COMPOSABLE

private val EMPTY_COMPOSABLE: @Composable (Any?, Modifier) -> Unit = { _, _ -> }

@OptIn(ExperimentalSharedTransitionApi::class)
private val DefaultBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}