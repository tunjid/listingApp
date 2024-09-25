package com.tunjid.scaffold.treenav.adaptive.threepane.configurations

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.AdaptiveMovableSharedElementScope
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.LocalMovableSharedElementScope
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptivePaneState
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route

fun <S : Node, R : Node> AdaptiveNavHostConfiguration<ThreePane, S, R>.movableSharedElementConfiguration(
    movableSharedElementHostState: MovableSharedElementHostState<ThreePane, R>,
): AdaptiveNavHostConfiguration<ThreePane, S, R> = delegated { node ->
    val original = this@movableSharedElementConfiguration.configuration(node)
    AdaptiveNodeConfiguration(
        transitions = original.transitions,
        paneMapper = original.paneMapper,
        render = { inner ->
            val delegate = remember {
                AdaptiveMovableSharedElementScope(
                    paneScope = this,
                    movableSharedElementHostState = movableSharedElementHostState,
                )
            }
            delegate.paneScope = this

            val movableSharedElementScope = remember {
                ThreePaneMovableSharedElementScope(
                    hostState = movableSharedElementHostState,
                    delegate = delegate,
                )
            }

            CompositionLocalProvider(
                LocalMovableSharedElementScope provides movableSharedElementScope
            ) {
                original.render(this, inner)
            }
        },
    )
}

@Stable
private class ThreePaneMovableSharedElementScope<R : Node>(
    private val hostState: MovableSharedElementHostState<ThreePane, R>,
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
                paneScope.isPreviewingBack && hostState.isCurrentlyShared(key) -> { _, modifier ->
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

fun AdaptivePaneState<ThreePane, Route>?.canAnimateOnStartingFrames() =
    this?.pane != ThreePane.TransientPrimary

private val AdaptivePaneScope<ThreePane, *>.isPreviewingBack: Boolean
    get() = paneState.pane == ThreePane.Primary
            && paneState.adaptation == ThreePane.PrimaryToTransient