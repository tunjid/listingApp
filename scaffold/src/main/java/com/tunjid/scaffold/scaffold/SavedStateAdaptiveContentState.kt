package com.tunjid.scaffold.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.adaptive.AnimatedAdaptiveContentScope
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope
import com.tunjid.scaffold.adaptive.MovableSharedElementData
import com.tunjid.scaffold.adaptive.SharedElementOverlay
import com.tunjid.scaffold.di.AdaptiveRouter
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.lifecycle.LocalViewModelFactory
import com.tunjid.scaffold.lifecycle.NodeViewModelFactoryProvider
import com.tunjid.scaffold.lifecycle.NodeViewModelStoreCreator
import com.tunjid.treenav.MultiStackNav
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@AssistedFactory
interface AdaptiveContentStateFactory {
    fun create(
        scope: CoroutineScope,
        saveableStateHolder: SaveableStateHolder?,
    ): SavedStateAdaptiveContentState
}

@Stable
class SavedStateAdaptiveContentState @AssistedInject constructor(
    val adaptiveRouter: AdaptiveRouter,
    val nodeViewModelFactoryProvider: NodeViewModelFactoryProvider,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
    @Assisted coroutineScope: CoroutineScope,
    @Assisted saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentState,
    SaveableStateHolder by saveableStateHolder {

    private val mutator = coroutineScope.adaptiveNavigationStateMutator(
        adaptiveRouter = adaptiveRouter,
        navStateFlow = navStateFlow,
        uiStateFlow = uiStateFlow,
        onChanged = ::slotBasedAdaptiveNavigationState::set
    )

    internal fun onAction(action: Action) = mutator.accept(action)

    internal var slotBasedAdaptiveNavigationState: SlotBasedAdaptiveNavigationState by mutableStateOf(
        value = mutator.state.value
    )
    override val navigationState: Adaptive.NavigationState
        get() = slotBasedAdaptiveNavigationState

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Pane.slots.forEach { slot ->
                map[slot] = movableContentOf {
                    Render(slot)
                }
            }
        }

    override val overlays: Collection<SharedElementOverlay>
        get() = keysToMovableSharedElements.values

    private val keysToMovableSharedElements = mutableStateMapOf<Any, MovableSharedElementData<*>>()
    internal val nodeViewModelStoreCreator = NodeViewModelStoreCreator(
        rootNodeProvider = navStateFlow::value
    )

    @Composable
    override fun RouteIn(pane: Adaptive.Pane) {
        val slot = slotBasedAdaptiveNavigationState.slotFor(pane)
        slotsToRoutes.getValue(slot).invoke()
    }

    fun isCurrentlyShared(key: Any): Boolean =
        keysToMovableSharedElements.contains(key)

    fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit {
        val movableSharedElementData = keysToMovableSharedElements.getOrPut(key) {
            MovableSharedElementData(
                sharedElement = sharedElement,
                onRemoved = { keysToMovableSharedElements.remove(key) }
            )
        }
        // Can't really guarantee that the caller will use the same key for the right type
        return movableSharedElementData.moveableSharedElement
    }
}

/**
 * Renders [slot] into is [Adaptive.Pane] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun SavedStateAdaptiveContentState.Render(
    slot: Adaptive.Slot,
) {
    val paneTransition = updateTransition(
        targetState = slotBasedAdaptiveNavigationState.paneStateFor(slot),
        label = "$slot-PaneTransition",
    )
    paneTransition.AnimatedContent(
        contentKey = { it.currentNode?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetPaneState ->
        val scope = remember {
            AnimatedAdaptiveContentScope(
                paneState = targetPaneState,
                adaptiveContentHost = this@Render,
                animatedContentScope = this
            )
        }
        // While technically a backwards write, it stabilizes and ensures the values are
        // correct at first composition
        scope.paneState = targetPaneState

        when (val route = targetPaneState.currentNode) {
            null -> Unit
            else -> Box(
                modifier = modifierFor(
                    adaptiveRouter = adaptiveRouter,
                    paneState = targetPaneState,
                    windowSizeClass = navigationState.windowSizeClass
                )
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveContentScope provides scope,
                    LocalViewModelFactory provides nodeViewModelFactoryProvider.viewModelFactoryFor(
                        route
                    ),
                    LocalViewModelStoreOwner provides nodeViewModelStoreCreator.viewModelStoreOwnerFor(
                        route
                    ),
                ) {
                    SaveableStateProvider(route.id) {
                        adaptiveRouter.destination(route).invoke()
                        DisposableEffect(Unit) {
                            onDispose {
                                val routeIds = slotBasedAdaptiveNavigationState.nodeIds
                                if (!routeIds.contains(route.id)) removeState(route.id)
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
                onAction(Action.RouteExitStart(routeId))
            }
        }
        // Remove route ids that have animated out
        DisposableEffect(Unit) {
            onDispose {
                val routeId = targetPaneState.currentNode?.id ?: return@onDispose
                onAction(Action.RouteExitEnd(routeId))
                targetPaneState.currentNode?.let(nodeViewModelStoreCreator::clearStoreFor)
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.modifierFor(
    adaptiveRouter: AdaptiveRouter,
    paneState: Adaptive.PaneState,
    windowSizeClass: WindowSizeClass,
) = when (paneState.pane) {
    Adaptive.Pane.Primary, Adaptive.Pane.Secondary -> FillSizeModifier
        .background(color = MaterialTheme.colorScheme.surface)
        .then(
            when {
                windowSizeClass.minWidthDp > WindowSizeClass.COMPACT.minWidthDp -> Modifier.clip(
                    RoundedCornerShape(16.dp)
                )

                else -> Modifier
            }
        )
        .then(
            when (val enterAndExit = adaptiveRouter.transitionsFor(paneState)) {
                null -> Modifier
                else -> Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            }
        )

    Adaptive.Pane.TransientPrimary -> FillSizeModifier
        .backPreviewModifier()

    null -> FillSizeModifier
}

private val FillSizeModifier = Modifier.fillMaxSize()