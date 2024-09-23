/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.treenav.adaptive.threepane

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.Adaptation.Swap
import com.tunjid.treenav.adaptive.AdaptiveRouteConfiguration
import com.tunjid.treenav.adaptive.AdaptiveRouter

/**
 * A layout in the hierarchy that hosts an [AdaptiveRouteConfiguration]
 */
enum class ThreePane {
    Primary,
    TransientPrimary,
    Secondary,
    Tertiary,
    Overlay;

    companion object {
        val PrimaryToSecondary = Swap(
            from = Primary,
            to = Secondary
        )

        val SecondaryToPrimary = Swap(
            from = Secondary,
            to = Primary
        )

        val PrimaryToTransient = Swap(
            from = Primary,
            to = TransientPrimary
        )
    }
}

fun <R : Node> AdaptiveRouter<ThreePane, R>.adaptFor(
    windowSizeClassState: State<WindowSizeClass>,
) = object : AdaptiveRouter<ThreePane, R> by this {
    override fun configuration(node: R): AdaptiveRouteConfiguration<ThreePane, R> {
        val original = this@adaptFor.configuration(node)
        return AdaptiveRouteConfiguration(
            render = original.render,
            transitions = original.transitions,
            paneMapper = { inner ->
                // Consider navigation state different if window size class changes
                val windowSizeClass by windowSizeClassState
                val originalMapping = original.paneMapping(inner)
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
}
