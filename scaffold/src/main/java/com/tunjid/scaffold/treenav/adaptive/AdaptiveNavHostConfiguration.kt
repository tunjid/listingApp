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

package com.tunjid.treenav.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

@Stable
class AdaptiveNavHostConfiguration<T, S : Node, R : Node> internal constructor(
    val navigationState: State<S>,
    val currentDestination: State<R>,
    val strategy: (node: R) -> AdaptivePaneStrategy<T, R>
)

/**
 * Configures an [AdaptiveNavHost] for rendering destinations into different panes.
 *
 * @param navigationState the navigation state to be adapted into various panes.
 * @param currentDestination the current destination in the [navigationState].
 * @param strategy the strategy used to adapt the [currentDestination] for the available panes.
 */
fun <T, S : Node, R : Node> adaptiveNavHostConfiguration(
    navigationState: State<S>,
    currentDestination: State<R>,
    strategy: (destination: R) -> AdaptivePaneStrategy<T, R>
) = AdaptiveNavHostConfiguration(
    navigationState = navigationState,
    currentDestination = currentDestination,
    strategy = strategy,
)

fun <T, S : Node, R : Node> AdaptiveNavHostConfiguration<T, S, R>.delegated(
    navigationState: State<S> = this@delegated.navigationState,
    currentNode: State<R> = this@delegated.currentDestination,
    strategy: (destination: R) -> AdaptivePaneStrategy<T, R>
) = AdaptiveNavHostConfiguration(
    navigationState = navigationState,
    currentDestination = currentNode,
    strategy = strategy,
)

/**
 * The current destination in a given [paneScope].
 */
@Composable
internal fun <T, R : Node> AdaptiveNavHostConfiguration<T, *, R>.Destination(
    paneScope: AdaptivePaneScope<T, R>
) {
    val current = remember(paneScope.paneState.currentNode) {
        paneScope.paneState.currentNode
    } ?: return
    with(strategy(current)) {
        val enterAndExit = transitions(paneScope)
        with(paneScope) {
            Box(
                modifier = Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            ) {
                paneScope.render(current)
            }
        }
    }
}

/**
 * THe current pane mapping to use in the [AdaptiveNavHost].
 */
@Composable
internal fun <T, R : Node> AdaptiveNavHostConfiguration<T, *, R>.paneMapping(): Map<T, R?> {
    val current by currentDestination
    return current.let {
        strategy(it).paneMapper(it)
    }
}