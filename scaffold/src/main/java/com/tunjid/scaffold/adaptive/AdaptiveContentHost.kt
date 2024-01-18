package com.tunjid.scaffold.adaptive

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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.scaffold.di.AdaptiveRouter
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.scaffold.backPreviewModifier
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Stable
internal interface AdaptiveContentHost {

    val adaptedState: Adaptive.NavigationState

    val hasAnimatingSharedElements: Boolean

    @Composable
    fun RouteIn(container: Adaptive.Container?)

    fun isCurrentlyShared(key: Any): Boolean

    fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit
}

@Composable
internal fun SavedStateAdaptiveContentHost(
    adaptiveRouter: AdaptiveRouter,
    navState: StateFlow<MultiStackNav>,
    uiState: StateFlow<UiState>,
    content: @Composable AdaptiveContentHost.() -> Unit
) {
    // Root LookaheadScope used to anchor all shared element transitions
    LookaheadScope {
        val coroutineScope = rememberCoroutineScope()
        val saveableStateHolder = rememberSaveableStateHolder()
        val adaptiveContentHost = remember(saveableStateHolder) {
            SavedStateAdaptiveContentHost(
                coroutineScope = coroutineScope,
                adaptiveRouter = adaptiveRouter,
                navStateFlow = navState,
                uiStateFlow = uiState,
                saveableStateHolder = saveableStateHolder
            )
        }

        LaunchedEffect(adaptiveContentHost) {
            adaptiveContentHost.update()
        }

        adaptiveContentHost.content()
    }
}

@Stable
private class SavedStateAdaptiveContentHost(
    val adaptiveRouter: AdaptiveRouter,
    navStateFlow: StateFlow<MultiStackNav>,
    uiStateFlow: StateFlow<UiState>,
    coroutineScope: CoroutineScope,
    saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentHost,
    SaveableStateHolder by saveableStateHolder,
    ActionStateProducer<Action, StateFlow<Adaptive.NavigationState>>
    by coroutineScope.adaptiveNavigationStateMutator(
        adaptiveRouter = adaptiveRouter,
        navStateFlow = navStateFlow,
        uiStateFlow = uiStateFlow
    ) {

    override var adaptedState by mutableStateOf(Adaptive.NavigationState.Initial)
        private set

    override val hasAnimatingSharedElements: Boolean
        get() = keysToSharedElements.values.any(SharedElementData<*>::isRunning)

    private val slotsToRoutes =
        mutableStateMapOf<Adaptive.Slot?, @Composable () -> Unit>().also { map ->
            map[null] = {}
            Adaptive.Container.slots.forEach { slot ->
                map[slot] = movableContentOf {
                    Render(slot)
                }
            }
        }

    private val keysToSharedElements = mutableStateMapOf<Any, SharedElementData<*>>()

    @Composable
    override fun RouteIn(container: Adaptive.Container?) {
        val slot = container?.let(adaptedState::slotFor)
        slotsToRoutes.getValue(slot).invoke()
    }

    override fun isCurrentlyShared(key: Any): Boolean =
        keysToSharedElements.contains(key)

    override fun <T> createOrUpdateSharedElement(
        key: Any,
        sharedElement: @Composable (T, Modifier) -> Unit,
    ): @Composable (T, Modifier) -> Unit {
        val sharedElementData = keysToSharedElements.getOrPut(key) {
            SharedElementData(
                sharedElement = sharedElement,
                onRemoved = { keysToSharedElements.remove(key) }
            )
        }
        // Can't really guarantee that the caller will use the same key for the right type
        return sharedElementData.moveableSharedElement
    }

    suspend fun update(): Unit = state.collect(::adaptedState::set)
}

/**
 * Renders [slot] into is [Adaptive.Container] with scopes that allow for animations
 * and shared elements.
 */
@Composable
private fun SavedStateAdaptiveContentHost.Render(
    slot: Adaptive.Slot,
) {
    val containerTransition = updateTransition(
        targetState = adaptedState.containerStateFor(slot),
        label = "$slot-ContainerTransition",
    )
    containerTransition.AnimatedContent(
        contentKey = { it.currentRoute?.id },
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { targetContainerState ->
        val scope = remember {
            AnimatedAdaptiveContentScope(
                containerState = targetContainerState,
                adaptiveContentHost = this@Render,
                animatedContentScope = this
            )
        }
        // While technically a backwards write, it stabilizes and ensures the values are
        // correct at first composition
        scope.containerState = targetContainerState

        when (val route = targetContainerState.currentRoute) {
            null -> Unit
            else -> Box(
                modifier = modifierFor(
                    adaptiveRouter = adaptiveRouter,
                    containerState = targetContainerState,
                    windowSizeClass = adaptedState.windowSizeClass
                )
            ) {
                CompositionLocalProvider(
                    LocalAdaptiveContentScope provides scope
                ) {
                    SaveableStateProvider(route.id) {
                        adaptiveRouter.screenComposable(route).invoke()
                        DisposableEffect(Unit) {
                            onDispose {
                                val backstackIds = adaptedState.backStackIds
                                if (!backstackIds.contains(route.id)) removeState(route.id)
                            }
                        }
                    }
                }
            }
        }

        // Add routes ids that are animating out
        LaunchedEffect(transition.isRunning) {
            if (transition.targetState == EnterExitState.PostExit) {
                val routeId = targetContainerState.currentRoute?.id ?: return@LaunchedEffect
                accept(Action.RouteExitStart(routeId))
            }
        }
        // Remove route ids that have animated out
        DisposableEffect(Unit) {
            onDispose {
                val routeId = targetContainerState.currentRoute?.id ?: return@onDispose
                accept(Action.RouteExitEnd(routeId))
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.modifierFor(
    adaptiveRouter: AdaptiveRouter,
    containerState: Adaptive.ContainerState,
    windowSizeClass: WindowSizeClass,
) = when (containerState.container) {
    Adaptive.Container.Primary, Adaptive.Container.Secondary -> FillSizeModifier
        .background(color = MaterialTheme.colorScheme.surface)
        .then(
            when {
                windowSizeClass > WindowSizeClass.COMPACT -> Modifier.clip(
                    RoundedCornerShape(16.dp)
                )

                else -> Modifier
            }
        )
        .then(
            when (val enterAndExit = adaptiveRouter.transitionsFor(containerState)) {
                null -> Modifier
                else -> Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            }
        )

    Adaptive.Container.TransientPrimary -> FillSizeModifier
        .backPreviewModifier()

    null -> FillSizeModifier
}

private val FillSizeModifier = Modifier.fillMaxSize()