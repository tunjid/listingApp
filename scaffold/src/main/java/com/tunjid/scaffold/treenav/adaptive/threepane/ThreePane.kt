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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.Adaptation.Swap
import com.tunjid.treenav.adaptive.Adaptive
import com.tunjid.treenav.adaptive.AdaptiveConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.AdaptiveRouter

/**
 * A layout in the hierarchy that hosts an [AdaptiveConfiguration]
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

fun <S : Node, R : Node> AdaptiveRouter<ThreePane, S, R>.adaptFor(
    windowSizeClassState: State<WindowSizeClass>,
) = object : AdaptiveRouter<ThreePane, S, R> by this {
    override fun configuration(node: R): AdaptiveConfiguration<ThreePane, R> {
        val original = this@adaptFor.configuration(node)
        return AdaptiveConfiguration(
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

fun <R : Node> threePaneAdaptiveConfiguration(
    transitions: AdaptivePaneScope<ThreePane, R>.() -> Adaptive.Transitions = {
        val state = paneState
        when (state.pane) {
            ThreePane.Primary,
            ThreePane.Secondary -> when (state.adaptation) {
                ThreePane.PrimaryToSecondary,
                ThreePane.SecondaryToPrimary -> NoTransition

                else -> DefaultTransition
            }

            ThreePane.TransientPrimary -> when (state.adaptation) {
                ThreePane.PrimaryToTransient -> when (state.pane) {
                    ThreePane.Secondary -> DefaultTransition
                    else -> NoTransition
                }

                else -> DefaultTransition
            }

            else -> NoTransition
        }
    },
    paneMapping: @Composable (R) -> Map<ThreePane, R?> = {
        mapOf(ThreePane.Primary to it)
    },
    render: @Composable AdaptivePaneScope<ThreePane, R>.(R) -> Unit
) = AdaptiveConfiguration(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val RouteTransitionAnimationSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 700
)

private val DefaultTransition = Adaptive.Transitions(
    enter = fadeIn(
        animationSpec = RouteTransitionAnimationSpec,
        // This is needed because I can't exclude shared elements from transitions
        // so to actually see them move, start fading in from 0.1f
        initialAlpha = 0.1f
    ),
    exit = fadeOut(
        animationSpec = RouteTransitionAnimationSpec
    )
)

private val NoTransition = Adaptive.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)