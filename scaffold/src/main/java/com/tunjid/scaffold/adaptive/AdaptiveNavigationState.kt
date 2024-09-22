package com.tunjid.scaffold.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Stable
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.treenav.Node

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Pane]
     */
    @Stable
    internal interface PaneScope : AnimatedVisibilityScope, MovableSharedElementScope {

        /**
         * Unique key to identify this scope
         */
        val key: String

        val paneState: PaneState
    }

    /**
     * A layout in the hierarchy that hosts an [AdaptiveRouteConfiguration]
     */
    enum class Pane {
        Primary, Secondary, TransientPrimary;

        companion object {
            internal val slots = Pane.entries.indices.map(Adaptive::Slot)
        }
    }

    /**
     * A spot taken by an [AdaptiveRouteConfiguration] that may be moved in from [Pane] to [Pane]
     */
    @JvmInline
    value class Slot internal constructor(val index: Int)

    /**
     * Information about content in an [Adaptive.Pane]
     */
    @Stable
    sealed interface PaneState {
        val currentNode: Node?
        val previousNode: Node?
        val pane: Pane?
        val adaptation: Adaptation
    }

    internal val PaneState.key get() = "${currentNode?.id}-$pane"

    /**
     * Describes how a node transitions after an adaptive change
     */
    data class Transitions(
        val enter: EnterTransition,
        val exit: ExitTransition,
    )

    /**
     * [Slot] based implementation of [PaneState]
     */
    internal data class SlotPaneState(
        val slot: Slot?,
        override val currentNode: Node?,
        override val previousNode: Node?,
        override val pane: Pane?,
        override val adaptation: Adaptation,
    ) : PaneState

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
        data class Swap(
            val from: Pane,
            val to: Pane?,
        ) : Adaptation()

        operator fun Swap.contains(pane: Pane?) = pane == from || pane == to

        companion object {
            val PrimaryToSecondary = Swap(
                from = Pane.Primary,
                to = Pane.Secondary
            )

            val SecondaryToPrimary = Swap(
                from = Pane.Secondary,
                to = Pane.Primary
            )

            val PrimaryToTransient = Swap(
                from = Pane.Primary,
                to = Pane.TransientPrimary
            )
        }
    }

    interface NavigationState {

        val windowSizeClass: WindowSizeClass

        fun nodeFor(
            pane: Pane
        ): Node?

        fun adaptationIn(
            pane: Pane
        ): Adaptation?
    }

}