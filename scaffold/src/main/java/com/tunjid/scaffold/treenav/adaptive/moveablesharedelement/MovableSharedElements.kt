package com.tunjid.scaffold.treenav.adaptive.moveablesharedelement

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.Adaptive.key
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
    private val canAnimateOnStartingFrames: (AdaptivePaneState<T, R>) -> Boolean,
) {

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
        mutableStateMapOf<Any, MovableSharedElementData<*, T, R>>()

    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    fun <S> createOrUpdateSharedElement(
        paneScope: AdaptivePaneScope<T, R>,
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
        }.also { it.adaptivePaneScope = paneScope }

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

    var paneScope by mutableStateOf(paneScope)

    fun isCurrentlyShared(key: Any): Boolean =
        movableSharedElementHostState.isCurrentlyShared(key)

    @Composable
    override fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        // This pane state may be animating out. Look up the actual current route

        val isActive = when(paneScope.transition.targetState) {
            EnterExitState.PreEnter -> false
            EnterExitState.Visible -> true
            EnterExitState.PostExit -> false
        }

        // Do not use the shared element if this content is being animated out
        if (!isActive) return { _, _ -> }

        return movableSharedElementHostState.createOrUpdateSharedElement(
            paneScope = paneScope,
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
): @Composable (T, Modifier) -> Unit =
    LocalMovableSharedElementScope.current.movableSharedElementOf(
        key = key,
        sharedElement = sharedElement,
    )

val LocalMovableSharedElementScope =
    staticCompositionLocalOf<MovableSharedElementScope> {
        throw IllegalArgumentException("LocalMovableSharedElementScope not set")
    }

internal val LocalAdaptivePaneScope =
    staticCompositionLocalOf<AdaptivePaneScope<ThreePane, Route>?> {
        null
    }

