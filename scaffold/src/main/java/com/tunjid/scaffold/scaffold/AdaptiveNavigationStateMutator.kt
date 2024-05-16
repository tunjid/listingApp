package com.tunjid.scaffold.scaffold

import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.Adaptive.Pane.Primary
import com.tunjid.scaffold.adaptive.Adaptive.Pane.Secondary
import com.tunjid.scaffold.adaptive.Adaptive.Pane.TransientPrimary
import com.tunjid.scaffold.di.AdaptiveRouter
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.globalui.slices.routePaneState
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.traverse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

sealed class Action {
    data class RouteExitStart(val routeId: String) : Action()

    data class RouteExitEnd(val routeId: String) : Action()
}

internal fun CoroutineScope.adaptiveNavigationStateMutator(
    adaptiveRouter: AdaptiveRouter,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
    onChanged: (Adaptive.NavigationState) -> Unit
) = actionStateFlowMutator<Action, AppAdaptiveNavigationState>(
    initialState = AppAdaptiveNavigationState.Initial,
    started = SharingStarted.Eagerly,
    inputs = listOf(
        adaptiveNavigationStateMutations(
            adaptiveRouter= adaptiveRouter,
            navStateFlow = navStateFlow,
            uiStateFlow = uiStateFlow
        )
    ),
    actionTransform = { actions ->
        actions.toMutationStream {
            when (val action = type()) {
                is Action.RouteExitStart -> action.flow.routeExitStartMutations()
                is Action.RouteExitEnd -> action.flow.routeExitEndMutations()
            }
        }
    },
    stateTransform = { adaptiveNavFlow ->
        adaptiveNavFlow
            .filterNot(AppAdaptiveNavigationState::hasConflictingRoutes)
            .onEach(onChanged)
    }
)

/**
 * Adapts the [MultiStackNav] navigation state to one best optimized for display in the current
 * UI window configuration.
 */
private fun adaptiveNavigationStateMutations(
    adaptiveRouter: AdaptiveRouter,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>
): Flow<Mutation<AppAdaptiveNavigationState>> = combine(
    flow = navStateFlow,
    flow2 = uiStateFlow.distinctUntilChangedBy {
        listOf(it.backStatus.isPreviewing, it.routePaneState)
    },
    transform = { navState, uiState ->
        adaptiveNavigationState(
            adaptiveRouter = adaptiveRouter,
            multiStackNav = navState,
            uiState = uiState
        )
    }
)
    .distinctUntilChanged()
    .scan(
        initial = AppAdaptiveNavigationState.Initial.adaptTo(
            new = adaptiveNavigationState(
                adaptiveRouter = adaptiveRouter,
                multiStackNav = navStateFlow.value,
                uiState = uiStateFlow.value,
            )
        ),
        operation = AppAdaptiveNavigationState::adaptTo
    )
    .mapToMutation { newState ->
        // Replace the entire state except the knowledge of routes animating in and out
        newState.copy(routeIdsAnimatingOut = routeIdsAnimatingOut)
    }

private fun adaptiveNavigationState(
    adaptiveRouter: AdaptiveRouter,
    multiStackNav: MultiStackNav,
    uiState: UiState,
): AppAdaptiveNavigationState {
    // If there is a back preview in progress, show the back primary route in the
    // primary pane
    val primaryRoute = multiStackNav.primaryRouteOnBackPress.takeIf {
        uiState.backStatus.isPreviewing
    } ?: multiStackNav.primaryRoute

    // Parse the secondary route from the primary route
    val secondaryRoute = adaptiveRouter.secondaryRouteFor(primaryRoute)

    return AppAdaptiveNavigationState(
        panesToRoutes = mapOf(
            Primary to primaryRoute,
            Secondary to secondaryRoute.takeIf { route ->
                route?.id != primaryRoute.id
                        && uiState.windowSizeClass > WindowSizeClass.COMPACT
            },
            TransientPrimary to multiStackNav.primaryRoute.takeIf { route ->
                uiState.backStatus.isPreviewing
                        && route.id != primaryRoute.id
                        && route.id != secondaryRoute?.id
            },
        ),
        windowSizeClass = uiState.windowSizeClass,
        // Tentative, decide downstream
        swapAdaptations = emptySet(),
        backStackIds = mutableSetOf<String>().apply {
            multiStackNav.traverse(Order.DepthFirst) { add(it.id) }
        },
        // Tentative, decide downstream
        routeIdsAnimatingOut = emptySet(),
        // Tentative, decide downstream
        routeIdsToAdaptiveSlots = emptyMap(),
        // Tentative, decide downstream
        previousPanesToRoutes = emptyMap(),
        routePanePositionalState = uiState.routePaneState,
    )
}

/**
 * A method that adapts changes in navigation to different panes while allowing for them
 * to be animated easily.
 */
private fun AppAdaptiveNavigationState.adaptTo(
    new: AppAdaptiveNavigationState,
): AppAdaptiveNavigationState {
    val old = this

    val availableSlots = Adaptive.Pane.slots.toMutableSet()
    val unplacedRouteIds = new.panesToRoutes.values.mapNotNull { it?.id }.toMutableSet()

    val routeIdsToAdaptiveSlots = mutableMapOf<String?, Adaptive.Slot>()
    val swapAdaptations = mutableSetOf<Adaptive.Adaptation.Swap>()

    for ((toPane, toRoute) in new.panesToRoutes.entries) {
        if (toRoute == null) continue
        for ((fromPane, fromRoute) in old.panesToRoutes.entries) {
            if (toRoute.id != fromRoute?.id) continue
            val swap = Adaptive.Adaptation.Swap(
                from = fromPane,
                to = toPane
            )
            if (toPane != fromPane) {
                swapAdaptations.add(swap)
            }

            val fromRouteId = old.routeFor(swap.from)?.id
                ?.also(unplacedRouteIds::remove)
                ?: throw IllegalArgumentException("A swap cannot occur from a null route")

            val movedSlot = old.routeIdsToAdaptiveSlots[old.routeFor(swap.from)?.id]
                ?.also(availableSlots::remove)
                ?: throw IllegalArgumentException("A swap cannot occur from a null slot")

            routeIdsToAdaptiveSlots[fromRouteId] = movedSlot
            break
        }
    }

    unplacedRouteIds.forEach { routeId ->
        routeIdsToAdaptiveSlots[routeId] = availableSlots.first().also(availableSlots::remove)
    }

    return new.copy(
        swapAdaptations = when (old.panesToRoutes.mapValues { it.value?.id }) {
            new.panesToRoutes.mapValues { it.value?.id } -> old.swapAdaptations
            else -> swapAdaptations
        },
        previousPanesToRoutes = Adaptive.Pane.entries.associateWith(
            valueSelector = old::routeFor
        ),
        routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots,
    )
}

private fun Flow<Action.RouteExitStart>.routeExitStartMutations(): Flow<Mutation<AppAdaptiveNavigationState>> =
    mapToMutation { exitStart ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut + exitStart.routeId)
    }

private fun Flow<Action.RouteExitEnd>.routeExitEndMutations(): Flow<Mutation<AppAdaptiveNavigationState>> =
    mapToMutation { exitEnd ->
        copy(routeIdsAnimatingOut = routeIdsAnimatingOut - exitEnd.routeId).prune()
    }

/**
 * Checks if any of the new routes coming in has any conflicts with those animating out.
 */
private fun AppAdaptiveNavigationState.hasConflictingRoutes() =
    routeIdsAnimatingOut.contains(routeFor(Primary)?.id)
            || routeFor(Secondary)?.id?.let(routeIdsAnimatingOut::contains) == true
            || routeFor(TransientPrimary)?.id?.let(routeIdsAnimatingOut::contains) == true

/**
 * Trims unneeded metadata from the [Adaptive.NavigationState]
 */
private fun AppAdaptiveNavigationState.prune(): AppAdaptiveNavigationState = copy(
    routeIdsToAdaptiveSlots = routeIdsToAdaptiveSlots.filter { (routeId) ->
        if (routeId == null) return@filter false
        backStackIds.contains(routeId)
                || routeIdsAnimatingOut.contains(routeId)
                || previousPanesToRoutes.values.map { it?.id }.toSet().contains(routeId)
    },
    previousPanesToRoutes = previousPanesToRoutes.filter { (_, route) ->
        if (route == null) return@filter false
        backStackIds.contains(route.id)
                || routeIdsAnimatingOut.contains(route.id)
                || previousPanesToRoutes.values.map { it?.id }.toSet().contains(route.id)
    }
)

private val MultiStackNav.primaryRoute: Route
    get() = current as? Route ?: unknownRoute(path = "404")

private val MultiStackNav.primaryRouteOnBackPress: Route? get() = pop().current as? Route