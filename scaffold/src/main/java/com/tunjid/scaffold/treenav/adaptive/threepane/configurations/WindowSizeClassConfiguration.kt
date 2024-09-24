package com.tunjid.scaffold.treenav.adaptive.threepane.configurations

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.threepane.ThreePane

fun <S : Node, R : Node> AdaptiveNavHostConfiguration<ThreePane, S, R>.windowSizeClassConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
): AdaptiveNavHostConfiguration<ThreePane, S, R> = delegated { node ->
    val original = this@windowSizeClassConfiguration.configuration(node)
    AdaptiveNodeConfiguration(
        render = original.render,
        transitions = original.transitions,
        paneMapper = { inner ->
            // Consider navigation state different if window size class changes
            val windowSizeClass by windowSizeClassState
            val originalMapping = original.paneMapper(inner)
            val primaryNode = originalMapping[ThreePane.Primary]
            mapOf(
                ThreePane.Primary to primaryNode,
                ThreePane.Secondary to originalMapping[ThreePane.Secondary].takeIf { secondaryNode ->
                    secondaryNode?.id != primaryNode?.id
                            && windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
                },
            )
        }
    )
}