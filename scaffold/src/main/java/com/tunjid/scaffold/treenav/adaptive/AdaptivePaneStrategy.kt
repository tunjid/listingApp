package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.Node

/**
 * Provides adaptive strategy in panes [Pane] for a given navigation destination [Destination].
 */
@Stable
class AdaptivePaneStrategy<Pane, Destination : Node> internal constructor(
    val transitions: AdaptivePaneScope<Pane, Destination>.() -> AdaptivePaneScope.Transitions,
    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    val paneMapper: @Composable (Destination) -> Map<Pane, Destination?>,
    val render: @Composable AdaptivePaneScope<Pane, Destination>.(Destination) -> Unit
)

/**
 * Allows for defining the adaptation strategy in panes [Pane] for a given navigation destination [Destination].
 *
 * @param transitions the transitions to run within each [AdaptivePaneScope].
 * @param paneMapping provides the mapping of panes to destinations for a given destination [Destination].
 * @param render defines the Composable rendered for each destination
 * in a given [AdaptivePaneScope].
 */
fun <Pane, Destination : Node> adaptivePaneStrategy(
    transitions: AdaptivePaneScope<Pane, Destination>.() -> AdaptivePaneScope.Transitions = { NoTransition },
    paneMapping: @Composable (Destination) -> Map<Pane, Destination?> = { emptyMap() },
    render: @Composable AdaptivePaneScope<Pane, Destination>.(Destination) -> Unit
) = AdaptivePaneStrategy(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = AdaptivePaneScope.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)