package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Immutable
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.Adaptive.Adaptation.Change.contains
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.slices.RoutePanePositionalState
import com.tunjid.scaffold.globalui.slices.routePaneState
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.strings.Route

/**
 * Data structure for managing navigation as it adapts to various layout configurations
 */
@Immutable
internal data class AppAdaptiveNavigationState(
    /**
     * Moves between panes within a navigation sequence.
     */
    val swapAdaptations: Set<Adaptive.Adaptation.Swap>,
    /**
     * A mapping of [Pane] to the routes in them
     */
    val panesToRoutes: Map<Adaptive.Pane, Route?>,
    /**
     * A mapping of route ids to the adaptive slots they are currently in.
     */
    val routeIdsToAdaptiveSlots: Map<String?, Adaptive.Slot>,
    /**
     * A mapping of adaptive pane to the routes that were last in them.
     */
    val previousPanesToRoutes: Map<Adaptive.Pane, Route?>,
    /**
     * A set of route ids that may be returned to.
     */
    val backStackIds: Set<String>,
    /**
     * A set of route ids that are animating out.
     */
    val routeIdsAnimatingOut: Set<String>,
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
        internal val Initial = AppAdaptiveNavigationState(
            swapAdaptations = emptySet(),
            windowSizeClass = WindowSizeClass.COMPACT,
            panesToRoutes = mapOf(
                Adaptive.Pane.Primary to unknownRoute(
                    Adaptive.Pane.slots.first().toString()
                )
            ),
            routeIdsToAdaptiveSlots = Adaptive.Pane.slots.associateBy(Adaptive.Slot::toString),
            backStackIds = emptySet(),
            routeIdsAnimatingOut = emptySet(),
            previousPanesToRoutes = emptyMap(),
            routePanePositionalState = UiState().routePaneState,
        )
    }

    override val routeIds: Collection<String>
        get() = backStackIds

    override fun paneStateFor(
        slot: Adaptive.Slot
    ): Adaptive.PaneState {
        val route = routeFor(slot)
        val pane = route?.let(::paneFor)
        return Adaptive.SlotPaneState(
            slot = slot,
            currentRoute = route,
            previousRoute = previousPanesToRoutes[pane],
            pane = pane,
            adaptation = swapAdaptations.firstOrNull { pane in it }
                ?: Adaptive.Adaptation.Change,
        )
    }

    override fun slotFor(
        pane: Adaptive.Pane?
    ): Adaptive.Slot? = when (pane) {
        null -> null
        else -> routeIdsToAdaptiveSlots[panesToRoutes[pane]?.id]
    }

    override fun paneFor(
        route: Route
    ): Adaptive.Pane? =
        panesToRoutes.firstNotNullOfOrNull { (pane, paneRoute) ->
            if (paneRoute?.id == route.id) pane else null
        }

    override fun routeFor(
        slot: Adaptive.Slot
    ): Route? = routeIdsToAdaptiveSlots.firstNotNullOfOrNull { (routeId, routeSlot) ->
        if (routeSlot == slot) panesToRoutes.firstNotNullOfOrNull { (_, route) ->
            if (route?.id == routeId) route
            else null
        }
        else null
    }

    override fun routeFor(
        pane: Adaptive.Pane
    ): Route? = panesToRoutes[pane]

    override fun adaptationIn(
        pane: Adaptive.Pane
    ): Adaptive.Adaptation? = swapAdaptations.firstOrNull { pane in it }
}