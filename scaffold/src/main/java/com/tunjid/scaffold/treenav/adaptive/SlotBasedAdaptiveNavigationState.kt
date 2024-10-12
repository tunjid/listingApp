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
internal data class SlotBasedAdaptiveNavigationState<Pane, Destination : Node>(
    /**
     * Moves between panes within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptation.Swap<Pane>>,
    /**
     * A mapping of [Pane] to the nodes in them
     */
    val panesToDestinations: Map<Pane, Destination?>,
    /**
     * A mapping of adaptive pane to the nodes that were last in them.
     */
    val previousPanesToDestinations: Map<Pane, Destination?>,
    /**
     * A mapping of node ids to the adaptive slots they are currently in.
     */
    val destinationIdsToAdaptiveSlots: Map<String?, Slot>,
    /**
     * A set of node ids that may be returned to.
     */
    val backStackIds: Set<String>,
    /**
     * A set of node ids that are animating out.
     */
    val destinationIdsAnimatingOut: Set<String>,
) : AdaptiveNavigationState<Pane, Destination> {
    companion object {
        internal fun <T, R : Node> initial(
            slots: Collection<Slot>,
        ): SlotBasedAdaptiveNavigationState<T, R> = SlotBasedAdaptiveNavigationState(
            swapAdaptations = emptySet(),
            panesToDestinations = emptyMap(),
            destinationIdsToAdaptiveSlots = slots.associateBy(
                keySelector = Slot::toString
            ),
            backStackIds = emptySet(),
            destinationIdsAnimatingOut = emptySet(),
            previousPanesToDestinations = emptyMap(),
        )
    }

    internal fun paneStateFor(
        slot: Slot
    ): AdaptivePaneState<Pane, Destination> {
        val node = destinationFor(slot)
        val pane = node?.let(::paneFor)
        return SlotPaneState(
            slot = slot,
            currentDestination = node,
            previousDestination = previousPanesToDestinations[pane],
            pane = pane,
            adaptations = pane?.let(::adaptationsIn) ?: emptySet(),
        )
    }

    internal fun slotFor(
        pane: Pane
    ): Slot? = destinationIdsToAdaptiveSlots[
        panesToDestinations[pane]?.id
    ]

    private fun paneFor(
        node: Node
    ): Pane? = panesToDestinations.firstNotNullOfOrNull { (pane, paneRoute) ->
        if (paneRoute?.id == node.id) pane else null
    }

    private fun destinationFor(
        slot: Slot
    ): Destination? = destinationIdsToAdaptiveSlots.firstNotNullOfOrNull { (nodeId, nodeSlot) ->
        if (nodeSlot == slot) panesToDestinations.firstNotNullOfOrNull { (_, node) ->
            if (node?.id == nodeId) node
            else null
        }
        else null
    }

    override fun destinationFor(
        pane: Pane
    ): Destination? = panesToDestinations[pane]

    override fun adaptationsIn(
        pane: Pane
    ): Set<Adaptation> =
        swapAdaptations.filter { pane in it }
            .let {
                if (it.isEmpty()) when (panesToDestinations[pane]?.id) {
                    previousPanesToDestinations[pane]?.id -> setOf(Adaptation.Same)
                    else -> setOf(Adaptation.Change)
                }
                else it.toSet()
            }
}

/**
 * A method that adapts changes in navigation to different panes while allowing for them
 * to be animated easily.
 */
internal fun <T, R : Node> SlotBasedAdaptiveNavigationState<T, R>.adaptTo(
    slots: Set<Slot>,
    panesToNodes: Map<T, R?>,
    backStackIds: Set<String>,
): SlotBasedAdaptiveNavigationState<T, R> {
    val previous = this

    val previouslyUsedSlots = previous.destinationIdsToAdaptiveSlots
        .filter { it.key != null }
        .values
        .toSet()

    // Sort by most recently used to makes sure most recently used slots
    // are reused so animations run.
    val availableSlots = slots
        .sortedByDescending(previouslyUsedSlots::contains)
        .toMutableSet()

    val unplacedNodeIds = panesToNodes.values.mapNotNull { it?.id }.toMutableSet()

    val nodeIdsToAdaptiveSlots = mutableMapOf<String?, Slot>()
    val swapAdaptations = mutableSetOf<Adaptation.Swap<T>>()

    // Process nodes that swapped panes from old to new
    for ((toPane, toNode) in panesToNodes.entries) {
        if (toNode == null) continue
        for ((fromPane, fromNode) in previous.panesToDestinations.entries) {
            // Find a previous node from the last state
            if (toNode.id != fromNode?.id) continue
            val swap = Adaptation.Swap(
                from = fromPane,
                to = toPane
            )
            // The panes are different, a swap occurred
            if (toPane != fromPane) {
                swapAdaptations.add(swap)
            }

            // Since this node was swapped, preserve its existing slot
            val fromNodeId = checkNotNull(fromNode.id)
            check(unplacedNodeIds.remove(fromNodeId)) {
                "A swap cannot have occurred if the node did not exist in the previous state"
            }
            val reusedSlot = previous.destinationIdsToAdaptiveSlots.getValue(fromNodeId)
            check(availableSlots.remove(reusedSlot)) {
                "A swap cannot have occurred if the node did not exist in the previous state"
            }
            nodeIdsToAdaptiveSlots[fromNodeId] = reusedSlot
            break
        }
    }

    // All swaps have been processed; place remaining changes nodes in slots available.
    unplacedNodeIds.forEach { nodeId ->
        nodeIdsToAdaptiveSlots[nodeId] = availableSlots.first().also(availableSlots::remove)
    }

    return SlotBasedAdaptiveNavigationState(
        // If the values of the nodes to panes are the same, no swaps occurred.
        swapAdaptations = when (previous.panesToDestinations.mapValues { it.value?.id }) {
            panesToNodes.mapValues { it.value?.id } -> previous.swapAdaptations
            else -> swapAdaptations
        },
        previousPanesToDestinations = previous.panesToDestinations.keys.associateWith(
            valueSelector = previous::destinationFor
        ),
        destinationIdsToAdaptiveSlots = nodeIdsToAdaptiveSlots,
        backStackIds = backStackIds,
        panesToDestinations = panesToNodes,
        destinationIdsAnimatingOut = previous.destinationIdsAnimatingOut,
    )

}

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
internal fun <Pane, Destination : Node> SlotBasedAdaptiveNavigationState<Pane, Destination>.hasConflictingRoutes(): Boolean =
    panesToDestinations.keys
        .map(::destinationFor)
        .any {
            it?.id?.let(destinationIdsAnimatingOut::contains) == true
        }

/**
 * Trims unneeded metadata from the [AdaptiveNavigationState]
 */
internal fun <Pane, Destination : Node> SlotBasedAdaptiveNavigationState<Pane, Destination>.prune(): SlotBasedAdaptiveNavigationState<Pane, Destination> =
    copy(
        destinationIdsToAdaptiveSlots = destinationIdsToAdaptiveSlots.filter { (routeId) ->
            if (routeId == null) return@filter false
            backStackIds.contains(routeId)
                    || destinationIdsAnimatingOut.contains(routeId)
                    || previousPanesToDestinations.values.map { it?.id }.toSet().contains(routeId)
        },
        previousPanesToDestinations = previousPanesToDestinations.filter { (_, route) ->
            if (route == null) return@filter false
            backStackIds.contains(route.id)
                    || destinationIdsAnimatingOut.contains(route.id)
                    || previousPanesToDestinations.values.map { it?.id }.toSet().contains(route.id)
        }
    )