package com.tunjid.treenav.adaptive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.tunjid.treenav.Node
import kotlin.jvm.JvmInline

interface AdaptiveNavigationState<T, R : Node> {

    fun nodeFor(
        pane: T,
    ): R?

    fun adaptationIn(
        pane: T,
    ): Adaptation?
}

/**
 * A description of the process that the layout undertook to adapt to its new configuration
 */
sealed class Adaptation {
    /**
     * Routes were changed in panes
     */
    data object Change : Adaptation()

    /**
     * Routes were swapped in between panes
     */
    data class Swap<T>(
        val from: T,
        val to: T?,
    ) : Adaptation()

    operator fun <T> Swap<T>.contains(pane: T?): Boolean = pane == from || pane == to

}

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {


    /**
     * A spot taken by an [AdaptiveRouteConfiguration] that may be moved in from [Pane] to [Pane]
     */
    @JvmInline
    value class Slot internal constructor(val index: Int)

    internal val AdaptivePaneState<*, *>.key get() = "${currentNode?.id}-$pane"

    /**
     * Describes how a node transitions after an adaptive change
     */
    data class Transitions(
        val enter: EnterTransition,
        val exit: ExitTransition,
    )

    /**
     * [Slot] based implementation of [AdaptivePaneState]
     */
    internal data class SlotPaneState<T, R : Node>(
        val slot: Slot?,
        override val currentNode: R?,
        override val previousNode: R?,
        override val pane: T?,
        override val adaptation: Adaptation,
    ) : AdaptivePaneState<T, R>

}

