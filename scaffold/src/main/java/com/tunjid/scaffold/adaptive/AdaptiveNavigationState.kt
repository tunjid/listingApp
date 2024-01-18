package com.tunjid.scaffold.adaptive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.adaptive.Adaptive.Adaptation.Change.contains
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.slices.RouteContainerPositionalState
import com.tunjid.scaffold.globalui.slices.routeContainerState
import com.tunjid.scaffold.navigation.UnknownRoute
import com.tunjid.treenav.strings.Route

/**
 * Namespace for adaptive layout changes in an app
 */
object Adaptive {

    /**
     * Scope for adaptive content that can show up in an arbitrary [Container]
     */
    @Stable
    interface ContainerScope : AnimatedVisibilityScope {

        /**
         * Unique key to identify this scope
         */
        val key: String

        val containerState: ContainerState

        fun isCurrentlyShared(key: Any): Boolean

        @Composable
        fun <T> sharedElementOf(
            key: Any,
            sharedElement: @Composable (T, Modifier) -> Unit
        ): @Composable (T, Modifier) -> Unit
    }

    /**
     * A layout in the hierarchy that hosts an [AdaptiveRouteConfiguration]
     */
    enum class Container {
        Primary, Secondary, TransientPrimary;

        companion object {
            internal val slots = Container.entries.indices.map(Adaptive::Slot)
        }
    }

    /**
     * A spot taken by an [AdaptiveRouteConfiguration] that may be moved in from [Container] to [Container]
     */
    @JvmInline
    internal value class Slot(val index: Int)

    /**
     * Information about content in an [Adaptive.Container]
     */
    @Stable
    sealed interface ContainerState {
        val currentRoute: Route?
        val previousRoute: Route?
        val container: Container?
        val adaptation: Adaptation
    }

    internal val ContainerState.key get() = "${currentRoute?.id}-$container"

    /**
     * Describes how a route transitions after an adaptive change
     */
    data class Transitions(
        val enter: EnterTransition,
        val exit: ExitTransition,
    )

    /**
     * [Slot] based implementation of [ContainerState]
     */
    internal data class SlotContainerState(
        val slot: Slot?,
        override val currentRoute: Route?,
        override val previousRoute: Route?,
        override val container: Container?,
        override val adaptation: Adaptation,
    ) : ContainerState

    /**
     * A description of the process that the layout undertook to adapt to its new configuration
     */
    sealed class Adaptation {
        /**
         * Routes were changed in containers
         */
        data object Change : Adaptation()

        /**
         * Routes were swapped in between containers
         */
        data class Swap(
            val from: Container,
            val to: Container?,
        ) : Adaptation()

        operator fun Swap.contains(container: Container?) = container == from || container == to

        companion object {
            val PrimaryToSecondary = Swap(
                from = Container.Primary,
                to = Container.Secondary
            )

            val SecondaryToPrimary = Swap(
                from = Container.Secondary,
                to = Container.Primary
            )

            val PrimaryToTransient = Swap(
                from = Container.Primary,
                to = Container.TransientPrimary
            )
        }
    }

    /**
     * Data structure for managing navigation as it adapts to various layout configurations
     */
    @Immutable
    internal data class NavigationState(
        /**
         * Moves between containers within a navigation sequence.
         */
        val swapAdaptations: Set<Adaptation.Swap>,
        /**
         * A mapping of [Container] to the routes in them
         */
        val containersToRoutes: Map<Container, Route?>,
        /**
         * A mapping of route ids to the adaptive slots they are currently in.
         */
        val routeIdsToAdaptiveSlots: Map<String?, Slot>,
        /**
         * A mapping of adaptive container to the routes that were last in them.
         */
        val previousContainersToRoutes: Map<Container, Route?>,
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
        val windowSizeClass: WindowSizeClass,
        /**
         * The positionalState of route containers
         */
        val routeContainerPositionalState: RouteContainerPositionalState,
    ) {
        companion object {
            internal val Initial = NavigationState(
                swapAdaptations = emptySet(),
                windowSizeClass = WindowSizeClass.COMPACT,
                containersToRoutes = mapOf(
                    Container.Primary to UnknownRoute(Container.slots.first().toString())
                ),
                routeIdsToAdaptiveSlots = Container.slots.associateBy(Slot::toString),
                backStackIds = emptySet(),
                routeIdsAnimatingOut = emptySet(),
                previousContainersToRoutes = emptyMap(),
                routeContainerPositionalState = UiState().routeContainerState,
            )
        }
    }
}

internal fun Adaptive.NavigationState.containerStateFor(
    slot: Adaptive.Slot
): Adaptive.ContainerState {
    val route = routeFor(slot)
    val container = route?.let(::containerFor)
    return Adaptive.SlotContainerState(
        slot = slot,
        currentRoute = route,
        previousRoute = previousContainersToRoutes[container],
        container = container,
        adaptation = swapAdaptations.firstOrNull { container in it }
            ?: Adaptive.Adaptation.Change,
    )
}

internal fun Adaptive.NavigationState.slotFor(
    container: Adaptive.Container?
): Adaptive.Slot? = when (container) {
    null -> null
    else -> routeIdsToAdaptiveSlots[containersToRoutes[container]?.id]
}

internal fun Adaptive.NavigationState.containerFor(
    route: Route
): Adaptive.Container? = containersToRoutes.firstNotNullOfOrNull { (container, containerRoute) ->
    if (containerRoute?.id == route.id) container else null
}

internal fun Adaptive.NavigationState.routeFor(
    slot: Adaptive.Slot
): Route? = routeIdsToAdaptiveSlots.firstNotNullOfOrNull { (routeId, routeSlot) ->
    if (routeSlot == slot) containersToRoutes.firstNotNullOfOrNull { (_, route) ->
        if (route?.id == routeId) route
        else null
    }
    else null
}

internal fun Adaptive.NavigationState.routeFor(
    container: Adaptive.Container
): Route? = containersToRoutes[container]
