package com.tunjid.treenav.adaptive

import com.tunjid.treenav.Node

/**
 * State providing details about data in each pane [T] it hosts.
 */
interface AdaptiveNavigationState<T, R : Node> {

    fun nodeFor(
        pane: T,
    ): R?

    fun adaptationIn(
        pane: T,
    ): Adaptation?
}

/**
 * A description of the process that the layout undertook to adapt to its new configuration.
 */
sealed class Adaptation {
    /**
     * Destinations were changed in panes
     */
    data object Change : Adaptation()

    /**
     * Destinations were swapped in between panes
     */
    data class Swap<T>(
        val from: T,
        val to: T?,
    ) : Adaptation()

    /**
     * Checks if a [Swap] [Adaptation] involved [pane].
     */
    operator fun <T> Swap<T>.contains(pane: T?): Boolean = pane == from || pane == to

}
