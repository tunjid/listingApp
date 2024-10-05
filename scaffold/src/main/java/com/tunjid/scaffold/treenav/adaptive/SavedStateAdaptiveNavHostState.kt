package com.tunjid.treenav.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.Order
import com.tunjid.treenav.adaptive.lifecycle.NodeViewModelStoreCreator
import com.tunjid.treenav.traverse


/**
 * A host for adaptive navigation for panes [Pane] and destinations [Destination].
 */
@Stable
interface AdaptiveNavHostState<Pane, Destination : Node> {

    /**
     * Creates the scope that provides context about individual panes [Pane] in an [AdaptiveNavHost].
     */
    @Composable
    fun scope(): AdaptiveNavHostScope<Pane, Destination>
}

/**
 * Scope that provides context about individual panes [Pane] in an [AdaptiveNavHost].
 */
@Stable
interface AdaptiveNavHostScope<Pane, Destination : Node> {

    @Composable
    fun Destination(
        pane: Pane
    )

    fun adaptationIn(
        pane: Pane,
    ): Adaptation?

    fun nodeFor(
        pane: Pane,
    ): Destination?
}

/**
 * An implementation of an [AdaptiveNavHostState] that provides a [SaveableStateHolder] for each
 * navigation destination that shows up in its panes.
 *
 * @param panes a list of panes that is possible to show in the [AdaptiveNavHost] in all
 * possible configurations. The panes should consist of enum class instances, or a sealed class
 * hierarchy of kotlin objects.
 * @param configuration the [AdaptiveNavHostConfiguration] that applies adaptive semantics and
 * strategies for each navigation destination shown in the [AdaptiveNavHost].
 */
@Stable
class SavedStateAdaptiveNavHostState<Pane, Destination : Node>(
    private val panes: List<Pane>,
    private val configuration: AdaptiveNavHostConfiguration<Pane, *, Destination>,
) : AdaptiveNavHostState<Pane, Destination> {

    @Composable
    override fun scope(): AdaptiveNavHostScope<Pane, Destination> {
        val navigationState by configuration.navigationState
        val panesToNodes = configuration.paneMapping()
        val saveableStateHolder = rememberSaveableStateHolder()

        val adaptiveContentScope = remember {
            SavedStateAdaptiveNavHostScope(
                panes = panes,
                navHostConfiguration = configuration,
                initialPanesToNodes = panesToNodes,
                saveableStateHolder = saveableStateHolder,
            )
        }

        LaunchedEffect(navigationState, panesToNodes) {
            adaptiveContentScope.onNewNavigationState(
                navigationState = navigationState,
                panesToNodes = panesToNodes
            )
        }

        return adaptiveContentScope
    }

    companion object {
        @Stable
        private class SavedStateAdaptiveNavHostScope<Pane, Destination : Node>(
            panes: List<Pane>,
            initialPanesToNodes: Map<Pane, Destination?>,
            saveableStateHolder: SaveableStateHolder,
            val navHostConfiguration: AdaptiveNavHostConfiguration<Pane, *, Destination>,
        ) : AdaptiveNavHostScope<Pane, Destination>, SaveableStateHolder by saveableStateHolder {

            private val nodeViewModelStoreCreator = NodeViewModelStoreCreator(
                rootNodeProvider = navHostConfiguration.navigationState::value
            )

            val slots = List(
                size = panes.size,
                init = ::Slot
            ).toSet()

            var adaptiveNavigationState by mutableStateOf(
                value = SlotBasedAdaptiveNavigationState.initial<Pane, Destination>(slots = slots).adaptTo(
                    slots = slots,
                    panesToNodes = initialPanesToNodes,
                    backStackIds = navHostConfiguration.navigationState.value.backStackIds(),
                )
            )

            private val slotsToRoutes =
                mutableStateMapOf<Slot?, @Composable () -> Unit>().also { map ->
                    map[null] = {}
                    slots.forEach { slot ->
                        map[slot] = movableContentOf { Render(slot) }
                    }
                }

            @Composable
            override fun Destination(pane: Pane) {
                val slot = adaptiveNavigationState.slotFor(pane)
                slotsToRoutes[slot]?.invoke()
            }

            override fun adaptationIn(
                pane: Pane
            ): Adaptation? = adaptiveNavigationState.adaptationIn(pane)

            override fun nodeFor(
                pane: Pane
            ): Destination? = adaptiveNavigationState.destinationFor(pane)

            fun onNewNavigationState(
                navigationState: Node,
                panesToNodes: Map<Pane, Destination?>,
            ) {
                updateAdaptiveNavigationState {
                    adaptTo(
                        slots = slots.toSet(),
                        panesToNodes = panesToNodes,
                        backStackIds = navigationState.backStackIds()
                    )
                }
            }

            /**
             * Renders [slot] into its pane with scopes that allow for animations
             * and shared elements.
             */
            @Composable
            private fun Render(
                slot: Slot,
            ) {
                val paneTransition = updateTransition(
                    targetState = adaptiveNavigationState.paneStateFor(slot),
                    label = "$slot-PaneTransition",
                )
                paneTransition.AnimatedContent(
                    contentKey = { it.currentDestination?.id },
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = EnterTransition.None,
                            initialContentExit = ExitTransition.None,
                            sizeTransform = null,
                        )
                    }
                ) { targetPaneState ->
                    val scope = remember {
                        AnimatedAdaptivePaneScope(
                            paneState = targetPaneState,
                            activeState = derivedStateOf {
                                val activePaneState = adaptiveNavigationState.paneStateFor(slot)
                                activePaneState.currentDestination?.id == targetPaneState.currentDestination?.id
                            },
                            animatedContentScope = this@AnimatedContent,
                        )
                    }

                    // While technically a backwards write, it stabilizes and ensures the values are
                    // correct at first composition
                    scope.paneState = targetPaneState

                    when (val destination = targetPaneState.currentDestination) {
                        null -> Unit
                        else -> CompositionLocalProvider(
                            LocalViewModelStoreOwner
                                    provides nodeViewModelStoreCreator.viewModelStoreOwnerFor(destination),
                        ) {
                            SaveableStateProvider(destination.id) {
                                navHostConfiguration.Destination(paneScope = scope)
                                DisposableEffect(Unit) {
                                    onDispose {
                                        val destinationIds = adaptiveNavigationState.destinationIds
                                        if (!destinationIds.contains(destination.id)) removeState(destination.id)
                                    }
                                }
                            }
                        }
                    }

                    // Add routes ids that are animating out
                    LaunchedEffect(transition.isRunning) {
                        if (transition.targetState == EnterExitState.PostExit) {
                            val destinationId = targetPaneState.currentDestination?.id ?: return@LaunchedEffect
                            updateAdaptiveNavigationState {
                                copy(destinationIdsAnimatingOut = destinationIdsAnimatingOut + destinationId)
                            }
                        }
                    }
                    // Remove route ids that have animated out
                    DisposableEffect(Unit) {
                        onDispose {
                            val routeId = targetPaneState.currentDestination?.id ?: return@onDispose
                            updateAdaptiveNavigationState {
                                copy(destinationIdsAnimatingOut = destinationIdsAnimatingOut - routeId).prune()
                            }
                            targetPaneState.currentDestination?.let(nodeViewModelStoreCreator::clearStoreFor)
                        }
                    }
                }
            }

            private inline fun updateAdaptiveNavigationState(
                block: SlotBasedAdaptiveNavigationState<Pane, Destination>.() -> SlotBasedAdaptiveNavigationState<Pane, Destination>
            ) {
                adaptiveNavigationState = adaptiveNavigationState.block()
            }
        }

        private fun Node.backStackIds() =
            mutableSetOf<String>().apply {
                traverse(Order.DepthFirst) { add(it.id) }
            }
    }
}
