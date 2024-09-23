package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.Node

/**
 * Route implementation with adaptive semantics
 */
@Stable
class AdaptiveNodeConfiguration<T, R : Node> internal constructor(
    val transitions: AdaptivePaneScope<T, R>.() -> Adaptive.Transitions,
    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    val paneMapper: @Composable (R) -> Map<T, R?>,
    val render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
)

fun <T, R : Node> adaptiveNodeConfiguration(
    transitions: AdaptivePaneScope<T, R>.() -> Adaptive.Transitions = { NoTransition },
    paneMapping: @Composable (R) -> Map<T, R?> = { emptyMap() },
    render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
) = AdaptiveNodeConfiguration(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = Adaptive.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)