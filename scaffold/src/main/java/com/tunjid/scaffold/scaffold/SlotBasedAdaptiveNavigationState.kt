package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Immutable
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.Adaptive.Adaptation.Change.contains
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.slices.RoutePanePositionalState
import com.tunjid.scaffold.globalui.slices.routePaneState
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.Node

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class SlotBasedAdaptiveNavigationState(
    /**
     * Moves between panes within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptive.Adaptation.Swap>,
    /**
     * A mapping of [Adaptive.Pane] to the nodes in them
     */
    val panesToNodes: Map<Adaptive.Pane, Node?>,
    /**
     * A mapping of node ids to the adaptive slots they are currently in.
     */
    val nodeIdsToAdaptiveSlots: Map<String?, Adaptive.Slot>,
    /**
     * A mapping of adaptive pane to the nodes that were last in them.
     */
    val previousPanesToRoutes: Map<Adaptive.Pane, Node?>,
    /**
     * A set of node ids that may be returned to.
     */
    val backStackIds: Set<String>,
    /**
     * A set of node ids that are animating out.
     */
    val nodeIdsAnimatingOut: Set<String>,
    /**
     * The window size class of the current screen configuration
     */
    override val windowSizeClass: WindowSizeClass,
    /**
     * The positionalState of route panes
     */
    val routePanePositionalState: RoutePanePositionalState,
) : Adaptive.NavigationState {
    companion object {
        internal val Initial = SlotBasedAdaptiveNavigationState(
            swapAdaptations = emptySet(),
            windowSizeClass = WindowSizeClass.COMPACT,
            panesToNodes = mapOf(
                Adaptive.Pane.Primary to unknownRoute(
                    Adaptive.Pane.slots.first().toString()
                )
            ),
            nodeIdsToAdaptiveSlots = Adaptive.Pane.slots.associateBy(Adaptive.Slot::toString),
            backStackIds = emptySet(),
            nodeIdsAnimatingOut = emptySet(),
            previousPanesToRoutes = emptyMap(),
            routePanePositionalState = UiState().routePaneState,
        )
    }

    internal val nodeIds: Collection<String>
        get() = backStackIds

    internal fun paneStateFor(
        slot: Adaptive.Slot
    ): Adaptive.PaneState {
        val route = nodeFor(slot)
        val pane = route?.let(::paneFor)
        return Adaptive.SlotPaneState(
            slot = slot,
            currentNode = route,
            previousNode = previousPanesToRoutes[pane],
            pane = pane,
            adaptation = swapAdaptations.firstOrNull { pane in it }
                ?: Adaptive.Adaptation.Change,
        )
    }

    internal fun slotFor(
        pane: Adaptive.Pane
    ): Adaptive.Slot? = nodeIdsToAdaptiveSlots[
        panesToNodes[pane]?.id
    ]

    private fun paneFor(
        node: Node
    ): Adaptive.Pane? =
        panesToNodes.firstNotNullOfOrNull { (pane, paneRoute) ->
            if (paneRoute?.id == node.id) pane else null
        }

    private fun nodeFor(
        slot: Adaptive.Slot
    ): Node? = nodeIdsToAdaptiveSlots.firstNotNullOfOrNull { (nodeId, nodeSlot) ->
        if (nodeSlot == slot) panesToNodes.firstNotNullOfOrNull { (_, node) ->
            if (node?.id == nodeId) node
            else null
        }
        else null
    }

    override fun nodeFor(
        pane: Adaptive.Pane
    ): Node? = panesToNodes[pane]

    override fun adaptationIn(
        pane: Adaptive.Pane
    ): Adaptive.Adaptation? = swapAdaptations.firstOrNull { pane in it }
}