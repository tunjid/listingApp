package com.tunjid.scaffold.treenav.adaptive.threepane.configurations

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.adaptivePaneStrategy
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.threepane.ThreePane

/**
 * An [AdaptiveNavHostConfiguration] that selectively displays panes for a [ThreePane] layout
 * based on the space available determined by the [WindowSizeClass].
 *
 * @param windowSizeClassState provides the current [WindowSizeClass] of the display.
 */
fun <S : Node, R : Node> AdaptiveNavHostConfiguration<ThreePane, S, R>.threePaneAdaptiveConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
): AdaptiveNavHostConfiguration<ThreePane, S, R> = delegated { node ->
    val originalStrategy = this@threePaneAdaptiveConfiguration.strategyTransform(node)
    adaptivePaneStrategy(
        render = originalStrategy.render,
        transitions = originalStrategy.transitions,
        paneMapping = { inner ->
            // Consider navigation state different if window size class changes
            val windowSizeClass by windowSizeClassState
            val originalMapping = originalStrategy.paneMapper(inner)
            val primaryNode = originalMapping[ThreePane.Primary]
            mapOf(
                ThreePane.Primary to primaryNode,
                ThreePane.Secondary to originalMapping[ThreePane.Secondary].takeIf { secondaryDestination ->
                    secondaryDestination?.id != primaryNode?.id
                            && windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
                },
                ThreePane.Tertiary to originalMapping[ThreePane.Tertiary].takeIf { tertiaryDestination ->
                    tertiaryDestination?.id != primaryNode?.id
                            && windowSizeClass.minWidthDp > TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP
                },
            )
        }
    )
}

private const val TERTIARY_PANE_MIN_WIDTH_BREAKPOINT_DP = 1200