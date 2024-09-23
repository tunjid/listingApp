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

import androidx.compose.runtime.Immutable
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.Adaptation.Change.contains

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class SlotBasedAdaptiveNavigationState<T, R : Node>(
    /**
     * Moves between panes within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptation.Swap<T>>,
    /**
     * A mapping of [T] to the nodes in them
     */
    val panesToNodes: Map<T, R?>,
    /**
     * A mapping of adaptive pane to the nodes that were last in them.
     */
    val previousPanesToNodes: Map<T, R?>,
    /**
     * A mapping of node ids to the adaptive slots they are currently in.
     */
    val nodeIdsToAdaptiveSlots: Map<String?, Adaptive.Slot>,
    /**
     * A set of node ids that may be returned to.
     */
    val backStackIds: Set<String>,
    /**
     * A set of node ids that are animating out.
     */
    val nodeIdsAnimatingOut: Set<String>,
) : AdaptiveNavigationState<T, R> {
    companion object {
        internal fun <T, R : Node> initial(
            slots: Collection<Adaptive.Slot>,
        ): SlotBasedAdaptiveNavigationState<T, R> = SlotBasedAdaptiveNavigationState(
            swapAdaptations = emptySet(),
            panesToNodes = emptyMap(),
            nodeIdsToAdaptiveSlots = slots.associateBy(
                keySelector = Adaptive.Slot::toString
            ),
            backStackIds = emptySet(),
            nodeIdsAnimatingOut = emptySet(),
            previousPanesToNodes = emptyMap(),
        )
    }

    internal val nodeIds: Collection<String>
        get() = backStackIds

    internal fun paneStateFor(
        slot: Adaptive.Slot
    ): AdaptivePaneState<T, R> {
        val node = nodeFor(slot)
        val pane = node?.let(::paneFor)
        return Adaptive.SlotPaneState(
            slot = slot,
            currentNode = node,
            previousNode = previousPanesToNodes[pane],
            pane = pane,
            adaptation = swapAdaptations.firstOrNull { pane in it }
                ?: Adaptation.Change,
        )
    }

    internal fun slotFor(
        pane: T
    ): Adaptive.Slot? = nodeIdsToAdaptiveSlots[
        panesToNodes[pane]?.id
    ]

    private fun paneFor(
        node: Node
    ): T? = panesToNodes.firstNotNullOfOrNull { (pane, paneRoute) ->
        if (paneRoute?.id == node.id) pane else null
    }

    private fun nodeFor(
        slot: Adaptive.Slot
    ): R? = nodeIdsToAdaptiveSlots.firstNotNullOfOrNull { (nodeId, nodeSlot) ->
        if (nodeSlot == slot) panesToNodes.firstNotNullOfOrNull { (_, node) ->
            if (node?.id == nodeId) node
            else null
        }
        else null
    }

    override fun nodeFor(
        pane: T
    ): R? = panesToNodes[pane]

    override fun adaptationIn(
        pane: T
    ): Adaptation? = swapAdaptations.firstOrNull { pane in it }
}

/**
 * A method that adapts changes in navigation to different panes while allowing for them
 * to be animated easily.
 */
internal fun <T, R : Node> SlotBasedAdaptiveNavigationState<T, R>.adaptTo(
    slots: Set<Adaptive.Slot>,
    panesToNodes: Map<T, R?>,
    backStackIds: Set<String>,
): SlotBasedAdaptiveNavigationState<T, R> {
    val old = this

    val availableSlots = slots.toMutableSet()
    val unplacedRouteIds = panesToNodes.values.mapNotNull { it?.id }.toMutableSet()

    val routeIdsToAdaptiveSlots = mutableMapOf<String?, Adaptive.Slot>()
    val swapAdaptations = mutableSetOf<Adaptation.Swap<T>>()

    for ((toPane, toNode) in panesToNodes.entries) {
        if (toNode == null) continue
        for ((fromPane, fromNode) in old.panesToNodes.entries) {
            if (toNode.id != fromNode?.id) continue
            val swap = Adaptation.Swap(
                from = fromPane,
                to = toPane
            )
            if (toPane != fromPane) {
                swapAdaptations.add(swap)
            }

            val fromNodeId = old.nodeFor(swap.from)?.id
                ?.also(unplacedRouteIds::remove)
                ?: throw IllegalArgumentException("A swap cannot occur from a null route")

            val movedSlot = old.nodeIdsToAdaptiveSlots[old.nodeFor(swap.from)?.id]
                ?.also(availableSlots::remove)
                ?: throw IllegalArgumentException("A swap cannot occur from a null slot")

            routeIdsToAdaptiveSlots[fromNodeId] = movedSlot
            break
        }
    }

    unplacedRouteIds.forEach { routeId ->
        routeIdsToAdaptiveSlots[routeId] = availableSlots.first().also(availableSlots::remove)
    }

    return SlotBasedAdaptiveNavigationState(
        swapAdaptations = when (old.panesToNodes.mapValues { it.value?.id }) {
            panesToNodes.mapValues { it.value?.id } -> old.swapAdaptations
            else -> swapAdaptations
        },
        previousPanesToNodes = old.previousPanesToNodes.keys.associateWith(
            valueSelector = old::nodeFor
        ),
        nodeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots,
        backStackIds = backStackIds,
        panesToNodes = panesToNodes,
        nodeIdsAnimatingOut = old.nodeIdsAnimatingOut,
    )

}

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
internal fun <T, R : Node> SlotBasedAdaptiveNavigationState<T, R>.hasConflictingRoutes(): Boolean =
    panesToNodes.keys
        .map(::nodeFor)
        .any {
            it?.id?.let(nodeIdsAnimatingOut::contains) == true
        }

/**
 * Trims unneeded metadata from the [Adaptive.NavigationState]
 */
internal fun <T, R : Node> SlotBasedAdaptiveNavigationState<T, R>.prune(): SlotBasedAdaptiveNavigationState<T, R> =
    copy(
        nodeIdsToAdaptiveSlots = nodeIdsToAdaptiveSlots.filter { (routeId) ->
            if (routeId == null) return@filter false
            backStackIds.contains(routeId)
                    || nodeIdsAnimatingOut.contains(routeId)
                    || previousPanesToNodes.values.map { it?.id }.toSet().contains(routeId)
        },
        previousPanesToNodes = previousPanesToNodes.filter { (_, route) ->
            if (route == null) return@filter false
            backStackIds.contains(route.id)
                    || nodeIdsAnimatingOut.contains(route.id)
                    || previousPanesToNodes.values.map { it?.id }.toSet().contains(route.id)
        }
    )