package com.tunjid.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AirplaneTicket
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.listing.scaffold.R
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.savedstate.EmptySavedState
import com.tunjid.scaffold.savedstate.InitialSavedState
import com.tunjid.scaffold.savedstate.SavedState
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    val stack: AppStack,
    val index: Int,
    val selected: Boolean
)

@Singleton
class PersistedNavigationStateHolder @Inject constructor(
    appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser,
) : NavigationStateHolder by appScope.actionStateFlowMutator(
    initialState = InitialNavigationState,
    started = SharingStarted.Eagerly,
    actionTransform = { navActions ->
        flow {
            // Restore saved nav from disk first
            val savedState = savedStateRepository.savedState
                // Wait for a non empty saved state to be read
                .first { it != InitialSavedState }

            val multiStackNav = when {
                savedState == EmptySavedState -> SignedInNavigationState
                else -> routeParser.parseMultiStackNav(savedState)
            }

            emit { multiStackNav }

            emitAll(
                navActions.mapToMutation { navMutation ->
                    navMutation(
                        ImmutableNavigationContext(
                            state = this,
                            routeParser = routeParser
                        )
                    )
                }
            )
        }
    },
    stateTransform = { navigationStateFlow ->
        // Save each new navigation state in parallel
        navigationStateFlow.onEach { navigationState ->
            appScope.persistNavigationState(
                navigationState = navigationState,
                savedStateRepository = savedStateRepository
            )
        }
    }
)

operator fun NavigationMutation.plus(
    edit: MultiStackNav.() -> MultiStackNav
): NavigationMutation = {
    edit(this@plus.invoke(this))
}

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun CoroutineScope.persistNavigationState(
    navigationState: MultiStackNav,
    savedStateRepository: SavedStateRepository,
) = launch {
    if (navigationState != InitialNavigationState) savedStateRepository.updateState {
        this.copy(navigation = navigationState.toSavedState())
    }
}

private fun RouteParser.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = SignedInNavigationState.name,
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = SignedInNavigationState.stacks.getOrNull(index)?.name
                                        ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute =
                                        parse(pathAndQueries = route) ?: unknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.navigation.activeNav
        )

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                }
        )
    },
)

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Articles.stackName,
            children = listOf(routeOf("/listings"))
        ),
    )
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        StackNav(
            name = AppStack.Articles.stackName,
            children = listOf(routeOf("/listings"))
        ),
        StackNav(
            name = AppStack.Projects.stackName,
            children = listOf(routeOf("/favorites"))
        ),
        StackNav(
            name = AppStack.Talks.stackName,
            children = listOf(routeOf("/explore"))
        ),
        StackNav(
            name = AppStack.Settings.stackName,
            children = listOf(routeOf("/messages"))
        ),
        StackNav(
            name = AppStack.Settings.stackName,
            children = listOf(routeOf("/profile"))
        ),
    )
)

enum class AppStack(
    val stackName: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    Articles(
        stackName = "listings-stack",
        titleRes = R.string.listings,
        icon = Icons.Rounded.Apartment,
    ),
    Projects(
        stackName = "favorites-stack",
        titleRes = R.string.favorites,
        icon = Icons.Rounded.FavoriteBorder,
    ),
    Talks(
        stackName = "explore-stack",
        titleRes = R.string.explore,
        icon = Icons.AutoMirrored.Rounded.AirplaneTicket,
    ),
    Settings(
        stackName = "messages-stack",
        titleRes = R.string.messages,
        icon = Icons.Rounded.ChatBubbleOutline,
    ),
    Profile(
        stackName = "profile-stack",
        titleRes = R.string.profile,
        icon = Icons.Rounded.AccountCircle,
    ),
}