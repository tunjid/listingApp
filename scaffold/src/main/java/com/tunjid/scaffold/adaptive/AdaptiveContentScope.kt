package com.tunjid.scaffold.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.scaffold.SavedStateAdaptiveContentState
import com.tunjid.treenav.adaptive.Adaptive.key
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptivePaneState
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route

/**
 * An implementation of [Adaptive.PaneScope] that supports animations and shared elements
 */
@Stable
internal class AnimatedAdaptiveContentScope(
    paneState: AdaptivePaneState<*, *>,
    val adaptiveContentHost: SavedStateAdaptiveContentState,
    val animatedContentScope: AnimatedContentScope
) : AnimatedVisibilityScope by animatedContentScope {

     val key: String by derivedStateOf { paneState.key }

     var paneState by mutableStateOf(paneState)

     fun isCurrentlyShared(key: Any): Boolean =
        adaptiveContentHost.isCurrentlyShared(key)

    @Composable
     fun <T> movableSharedElementOf(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit
    ): @Composable (T, Modifier) -> Unit {
        return sharedElement
//        val currentNavigationState = adaptiveContentHost.navigationState
//        // This pane state may be animating out. Look up the actual current route
//        val currentRouteInPane = paneState.pane?.let(
//            currentNavigationState::nodeFor
//        )
//        val isCurrentlyAnimatingIn = currentRouteInPane?.id == paneState.currentNode?.id
//
//        // Do not use the shared element if this content is being animated out
//        if (!isCurrentlyAnimatingIn) return { _, _ -> }
//
//        return adaptiveContentHost.createOrUpdateSharedElement(
//            key = key,
//            sharedElement = sharedElement
//        )
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
//    when (val scope = LocalAdaptiveContentScope.current) {
//        null -> throw IllegalArgumentException(
//            "This may only be called from an adaptive content scope"
//        )
//
//        else -> when (scope.paneState.pane) {
//            null -> throw IllegalArgumentException(
//                "Shared elements may only be used in non null panes"
//            )
//            // Allow shared elements in the primary or transient primary content only
//            Adaptive.Pane.Primary -> when {
//                // Show a blank space for shared elements between the destinations
//                scope.isPreviewingBack && scope.isCurrentlyShared(key) -> { _, modifier ->
//                    Box(modifier)
//                }
//                // If previewing and it won't be shared, show the item as is
//                scope.isPreviewingBack -> sharedElement
//                // Share the element
//                else -> scope.movableSharedElementOf(
//                    key = key,
//                    sharedElement = sharedElement
//                )
//            }
//            // Share the element when in the transient pane
//            Adaptive.Pane.TransientPrimary -> scope.movableSharedElementOf(
//                key = key,
//                sharedElement = sharedElement
//            )
//            // In the secondary pane use the element as is
//            Adaptive.Pane.Secondary -> sharedElement
//        }
//    }

internal val LocalAdaptivePaneScope = staticCompositionLocalOf<AdaptivePaneScope<ThreePane, Route>?> {
    null
}

@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    TODO()
}

