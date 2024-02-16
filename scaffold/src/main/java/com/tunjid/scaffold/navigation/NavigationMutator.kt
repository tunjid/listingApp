package com.tunjid.scaffold.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.mutationOf
import com.tunjid.scaffold.savedstate.SavedState
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.UrlRouteMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

typealias NavigationStateHolder = ActionStateMutator<@JvmSuppressWildcards NavigationMutation, @JvmSuppressWildcards StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation
}

data class NavItem(
    val name: String,
    val icon: ImageVector,
    val index: Int,
    val selected: Boolean
)

private val EmptyNavigationState = MultiStackNav(
    name = "emptyMultiStack",
    stacks = listOf(
        StackNav(
            name = "emptyStack",
            children = listOf(UnknownRoute())
        )
    )
)

@Singleton
class PersistedNavigationStateHolder @Inject constructor(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser<@JvmSuppressWildcards Route>,
    routeMatcherMap: Map<String, @JvmSuppressWildcards UrlRouteMatcher<Route>>
) : NavigationStateHolder by appScope.actionStateFlowMutator(
    initialState = EmptyNavigationState,
    started = SharingStarted.Eagerly,
    actionTransform = { navMutations ->
        flow {
            // Restore saved nav from disk first
            val savedState = savedStateRepository.savedState.first { !it.isEmpty }
            val multiStackNav = routeParser.parseMultiStackNav(savedState)

            println("KEYS: ${routeMatcherMap.keys}")

            emit { multiStackNav }
            emitAll(
                navMutations.map { navMutation ->
                    mutationOf {
                        navMutation(
                            ImmutableNavigationContext(
                                state = this,
                                routeParser = routeParser
                            )
                        )
                    }
                }
            )
        }
    },
)

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun RouteParser<Route>.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation
        .fold(
            initial = MultiStackNav(name = "AppNav"),
            operation = { multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = routesForStack.firstOrNull() ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute = parse(routeString = route) ?: UnknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.activeNav
        )