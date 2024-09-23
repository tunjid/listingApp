package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import com.tunjid.treenav.Node

/**
 * Route implementation with adaptive semantics
 */
class AdaptiveRouteConfiguration<T, R : Node> internal constructor(
    val transitions: AdaptivePaneScope<T, R>.() -> Adaptive.Transitions = { NoTransition },
    val paneMapper: @Composable (R) -> Map<T, R?> = { emptyMap() },
    val render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
) {

    @Composable
    fun AdaptivePaneScope<T, R>.Render(route: R) {
        render(route)
    }

    /**
     * Defines what route to show in the secondary panel alongside this route
     */
    @Composable
    fun paneMapping(
        node: R
    ): Map<T, R?> = paneMapper(node)

    fun AdaptivePaneScope<T, R>.transitionsFor(): Adaptive.Transitions = transitions()

}

fun <T, R : Node> adaptiveRouteConfiguration(
    transitions: AdaptivePaneScope<T, R>.() -> Adaptive.Transitions = { NoTransition },
    paneMapping: @Composable (R) -> Map<T, R?> = { emptyMap() },
    render: @Composable AdaptivePaneScope<T, R>.(R) -> Unit
) = AdaptiveRouteConfiguration(
    paneMapper = paneMapping,
    transitions = transitions,
    render = render
)

private val NoTransition = Adaptive.Transitions(
    enter = EnterTransition.None,
    exit = ExitTransition.None,
)