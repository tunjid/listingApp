package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.Node

/**
 * Provides adaptive strategy in panes [T] for a given navigation destination [R].
 */
@Stable
class AdaptivePaneStrategy<T, R : Node> internal constructor(
    val transitions: AdaptivePaneScope<T, R>.() -> AdaptivePaneScope.Transitions,
    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    val paneMapper: @Composable (R) -> Map<T, R?>,
    val render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
)

/**
 * Allows for defining the adaptation strategy in panes [T] for a given navigation destination [R].
 *
 * @param transitions the transitions to run within each [AdaptivePaneScope].
 * @param paneMapping provides the mapping of panes to destinations for a given destination [R].
 * @param render defines the Composable rendered for each destination
 * in a given [AdaptivePaneScope].
 */
fun <T, R : Node> adaptivePaneStrategy(
    transitions: AdaptivePaneScope<T, R>.() -> AdaptivePaneScope.Transitions = { NoTransition },
    paneMapping: @Composable (R) -> Map<T, R?> = { emptyMap() },
    render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
) = AdaptivePaneStrategy(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = AdaptivePaneScope.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)