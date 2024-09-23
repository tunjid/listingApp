package com.tunjid.treenav.adaptive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.tunjid.treenav.Node
import com.tunjid.treenav.Order
import com.tunjid.treenav.adaptive.lifecycle.NodeViewModelStoreCreator
import com.tunjid.treenav.traverse


@Stable
interface AdaptiveNavHostState<T, R : Node> {

    @Composable
    fun scope(): AdaptiveNavHostScope<T, R>
}

@Stable
interface AdaptiveNavHostScope<T, R : Node> {

    @Composable
    fun Destination(pane: T)

    fun adaptationIn(
        pane: T,
    ): Adaptation?

    fun nodeFor(
        pane: T,
    ): R?
}


@Stable
class SavedStateAdaptiveNavHostState<T, R : Node>(
    private val panes: List<T>,
    private val router: AdaptiveNavHostConfiguration<T, *, R>,
) : AdaptiveNavHostState<T, R> {

    @Composable
    override fun scope(): AdaptiveNavHostScope<T, R> {
        val navigationState by router.navigationState
        val panesToNodes = router.paneMapping()
        val saveableStateHolder = rememberSaveableStateHolder()

        val adaptiveContentScope = remember {
            SavedStateAdaptiveNavHostScope(
                panes = panes,
                router = router,
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
        private class SavedStateAdaptiveNavHostScope<T, R : Node>(
            panes: List<T>,
            initialPanesToNodes: Map<T, R?>,
            saveableStateHolder: SaveableStateHolder,
            val router: AdaptiveNavHostConfiguration<T, *, R>,
        ) : AdaptiveNavHostScope<T, R>, SaveableStateHolder by saveableStateHolder {

            private val nodeViewModelStoreCreator = NodeViewModelStoreCreator(
                rootNodeProvider = router.navigationState::value
            )

            val slots = List(panes.size, Adaptive::Slot).toSet()

            var adaptiveNavigationState by mutableStateOf(
                value = SlotBasedAdaptiveNavigationState.initial<T, R>(slots = slots).adaptTo(
                    slots = slots,
                    panesToNodes = initialPanesToNodes,
                    backStackIds = router.navigationState.value.backStackIds(),
                )
            )

            private val slotsToRoutes =
                mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
                    map[null] = {}
                    slots.forEach { slot ->
                        map[slot] = movableContentOf { Render(slot) }
                    }
                }

            @Composable
            override fun Destination(pane: T) {
                val slot = adaptiveNavigationState.slotFor(pane)
                slotsToRoutes[slot]?.invoke()
            }

            override fun adaptationIn(
                pane: T
            ): Adaptation? = adaptiveNavigationState.adaptationIn(pane)

            override fun nodeFor(
                pane: T
            ): R? = adaptiveNavigationState.nodeFor(pane)

            fun onNewNavigationState(
                navigationState: Node,
                panesToNodes: Map<T, R?>,
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
                slot: Adaptive.Slot,
            ) {
                val paneTransition = updateTransition(
                    targetState = adaptiveNavigationState.paneStateFor(slot),
                    label = "$slot-PaneTransition",
                )
                paneTransition.AnimatedContent(
                    contentKey = { it.currentNode?.id },
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
                            animatedContentScope = this@AnimatedContent,
                        )
                    }

                    // While technically a backwards write, it stabilizes and ensures the values are
                    // correct at first composition
                    scope.paneState = targetPaneState

                    when (val node = targetPaneState.currentNode) {
                        null -> Unit
                        else -> Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CompositionLocalProvider(
                                LocalViewModelStoreOwner provides nodeViewModelStoreCreator.viewModelStoreOwnerFor(
                                    node
                                ),
                            ) {
                                SaveableStateProvider(node.id) {
                                    router.Destination(paneScope = scope)
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            val routeIds = adaptiveNavigationState.nodeIds
                                            if (!routeIds.contains(node.id)) removeState(node.id)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add routes ids that are animating out
                    LaunchedEffect(transition.isRunning) {
                        if (transition.targetState == EnterExitState.PostExit) {
                            val routeId = targetPaneState.currentNode?.id ?: return@LaunchedEffect
                            updateAdaptiveNavigationState {
                                copy(nodeIdsAnimatingOut = nodeIdsAnimatingOut + routeId)
                            }
                        }
                    }
                    // Remove route ids that have animated out
                    DisposableEffect(Unit) {
                        onDispose {
                            val routeId = targetPaneState.currentNode?.id ?: return@onDispose
                            updateAdaptiveNavigationState {
                                copy(nodeIdsAnimatingOut = nodeIdsAnimatingOut - routeId).prune()
                            }
                            targetPaneState.currentNode?.let(nodeViewModelStoreCreator::clearStoreFor)
                        }
                    }
                }
            }

            private inline fun updateAdaptiveNavigationState(
                block: SlotBasedAdaptiveNavigationState<T, R>.() -> SlotBasedAdaptiveNavigationState<T, R>
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
