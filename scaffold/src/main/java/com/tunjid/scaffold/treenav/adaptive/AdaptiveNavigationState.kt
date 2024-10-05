package com.tunjid.treenav.adaptive

import com.tunjid.treenav.Node

/**
 * State providing details about data in each pane [Pane] it hosts.
 */
interface AdaptiveNavigationState<Pane, Destination : Node> {

    fun destinationFor(
        pane: Pane,
    ): Destination?

    fun adaptationIn(
        pane: Pane,
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
    data class Swap<Pane>(
        val from: Pane,
        val to: Pane?,
    ) : Adaptation()

    /**
     * Checks if a [Swap] [Adaptation] involved [pane].
     */
    operator fun <Pane> Swap<Pane>.contains(pane: Pane?): Boolean = pane == from || pane == to

}
