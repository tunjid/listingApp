package com.tunjid.scaffold.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.tunjid.scaffold.treenav.adaptive.moveableSharedElement.MovableSharedElementData
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHostScope
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptivePaneState
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route

internal interface SharedElementOverlay {
    fun ContentDrawScope.drawInOverlay()
}

interface MovableSharedElementScope {

    @Composable
    fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
class MovableSharedElementHostState<T, R : Node>(
    private val sharedTransitionScope: SharedTransitionScope,
    internal val adaptiveNavHostScope: AdaptiveNavHostScope<T, R>,
    private val canAnimateOnStartingFrames: (AdaptivePaneState<T, R>) -> Boolean,
) {

    internal val overlays: Collection<SharedElementOverlay>
        get() = keysToMovableSharedElements.values

    private val keysToMovableSharedElements =
        mutableStateMapOf<Any, MovableSharedElementData<*, T, R>>()

    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    fun <S> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (S, Modifier) -> Unit,
    ): @Composable (S, Modifier) -> Unit {
        val movableSharedElementData = keysToMovableSharedElements.getOrPut(key) {
            MovableSharedElementData(
                sharedTransitionScope = sharedTransitionScope,
                sharedElement = sharedElement,
                canAnimateOnStartingFrames = canAnimateOnStartingFrames,
                onRemoved = { keysToMovableSharedElements.remove(key) }
            )
        }

        // Can't really guarantee that the caller will use the same key for the right type
        return movableSharedElementData.moveableSharedElement
    }
}

/**
 * An implementation of [Adaptive.PaneScope] that supports animations and shared elements
 */
@Stable
internal class AdaptiveMovableSharedElementScope<T, R : Node>(
    paneScope: AdaptivePaneScope<T, R>,
    private val movableSharedElementHostState: MovableSharedElementHostState<T, R>,
) : MovableSharedElementScope {

    val key: String by derivedStateOf { paneScope.key }

    var paneScope by mutableStateOf(paneScope)

    fun isCurrentlyShared(key: Any): Boolean =
        movableSharedElementHostState.isCurrentlyShared(key)

    @Composable
    override fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        val paneState = paneScope.paneState
        // This pane state may be animating out. Look up the actual current route
        val currentRouteInPane = paneState.pane?.let(
            movableSharedElementHostState.adaptiveNavHostScope::nodeFor
        )
        val isCurrentlyAnimatingIn = currentRouteInPane?.id == paneState.currentNode?.id

        // Do not use the shared element if this content is being animated out
        if (!isCurrentlyAnimatingIn) return { _, _ -> }

        return movableSharedElementHostState.createOrUpdateSharedElement(
            key = key,
            sharedElement = sharedElement
        )
    }
}

/**
 * Creates a shared element composable that can be moved across compositions
 *
 * @param key the key for the shared element
 * @param sharedElement the element to be shared and moved
 */
@Composable
fun <T> movableSharedElementOf(
    key: Any,
    sharedElement: @Composable (T, Modifier) -> Unit
): @Composable (T, Modifier) -> Unit = sharedElement

@Stable
internal class ThreePaneMovableSharedElementScope<R : Node>(
    private val delegate: AdaptiveMovableSharedElementScope<ThreePane, R>,
) : MovableSharedElementScope {
    @Composable
    override fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        val paneScope = delegate.paneScope
        return when (paneScope.paneState.pane) {
            null -> throw IllegalArgumentException(
                "Shared elements may only be used in non null panes"
            )
            // Allow shared elements in the primary or transient primary content only
            ThreePane.Primary -> when {
                // Show a blank space for shared elements between the destinations
                paneScope.isPreviewingBack && delegate.isCurrentlyShared(key) -> { _, modifier ->
                    Box(modifier)
                }
                // If previewing and it won't be shared, show the item as is
                paneScope.isPreviewingBack -> sharedElement
                // Share the element
                else -> delegate.movableSharedElementOf(
                    key = key,
                    sharedElement = sharedElement
                )
            }
            // Share the element when in the transient pane
            ThreePane.TransientPrimary -> delegate.movableSharedElementOf(
                key = key,
                sharedElement = sharedElement
            )

            // In the other panes use the element as is
            ThreePane.Secondary,
            ThreePane.Tertiary,
            ThreePane.Overlay -> sharedElement
        }
    }
}

private fun AdaptivePaneState<ThreePane, Route>?.canAnimateOnStartingFrames() =
    this?.pane != ThreePane.TransientPrimary

private val AdaptivePaneScope<ThreePane, *>.isPreviewingBack: Boolean
    get() = paneState.pane == ThreePane.Primary
            && paneState.adaptation == ThreePane.PrimaryToTransient

internal val LocalAdaptivePaneScope =
    staticCompositionLocalOf<AdaptivePaneScope<ThreePane, Route>?> {
        null
    }

