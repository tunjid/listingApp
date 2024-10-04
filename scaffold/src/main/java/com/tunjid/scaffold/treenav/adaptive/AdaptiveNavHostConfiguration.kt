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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.treenav.Node

/**
 * Class for configuring an [AdaptiveNavHost] for adapting different navigation
 * destinations into different panes from an arbitrary [navigationState].
 *
 * @param navigationState the navigation state to be adapted into various panes.
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
@Stable
class AdaptiveNavHostConfiguration<T, S : Node, R : Node> internal constructor(
    val navigationState: State<S>,
    val destinationTransform: (S) -> R,
    val strategyTransform: (destination: R) -> AdaptivePaneStrategy<T, R>
) {
    internal val currentDestination: State<R> = derivedStateOf {
        destinationTransform(navigationState.value)
    }
}

/**
 * Provides an [AdaptiveNavHostConfiguration] for configuring an [AdaptiveNavHost] for
 * adapting different navigation destinations into different panes from an arbitrary
 * [navigationState].
 *
 * @param navigationState the navigation state to be adapted into various panes.
 * @param destinationTransform a transform of the [navigationState] to its current destination.
 * It is read inside a [derivedStateOf] block, so reads of snapshot
 * state objects will be observed.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
fun <T, S : Node, R : Node> adaptiveNavHostConfiguration(
    navigationState: State<S>,
    destinationTransform: (S) -> R,
    strategyTransform: (destination: R) -> AdaptivePaneStrategy<T, R>
) = AdaptiveNavHostConfiguration(
    navigationState = navigationState,
    destinationTransform = destinationTransform,
    strategyTransform = strategyTransform,
)

/**
 * Creates a new [AdaptiveNavHost] by delegating to [this] and  rendering destinations into different panes.
 *
 * @param destinationTransform a transform of [AdaptiveNavHostConfiguration.navigationState]
 * to its current destination. It is read inside a [derivedStateOf] block, so reads of snapshot
 * state objects will be observed.
 * @param strategyTransform provides the strategy used to adapt the current destination to the
 * panes available.
 */
fun <T, S : Node, R : Node> AdaptiveNavHostConfiguration<T, S, R>.delegated(
    destinationTransform: (S) -> R = this@delegated.destinationTransform,
    strategyTransform: (destination: R) -> AdaptivePaneStrategy<T, R>
) = adaptiveNavHostConfiguration(
    navigationState = this@delegated.navigationState,
    destinationTransform = destinationTransform,
    strategyTransform = strategyTransform,
)

/**
 * The current destination in a given [paneScope].
 */
@Composable
internal fun <T, R : Node> AdaptiveNavHostConfiguration<T, *, R>.Destination(
    paneScope: AdaptivePaneScope<T, R>
) {
    val current = remember(paneScope.paneState.currentDestination) {
        paneScope.paneState.currentDestination
    } ?: return
    with(strategyTransform(current)) {
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
        strategyTransform(it).paneMapper(it)
    }
}