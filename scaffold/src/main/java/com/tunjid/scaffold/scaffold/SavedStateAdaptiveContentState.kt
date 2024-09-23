package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.adaptive.MovableSharedElementData
import com.tunjid.scaffold.adaptive.SharedElementOverlay
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.lifecycle.LocalViewModelFactory
import com.tunjid.scaffold.lifecycle.NodeViewModelFactoryProvider
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Node
import com.tunjid.treenav.adaptive.AdaptiveConfiguration
import com.tunjid.treenav.adaptive.AdaptiveHostScope
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveConfiguration
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.adaptive.threepane.adaptFor
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import com.tunjid.treenav.adaptive.AdaptiveRouter as NewAdaptiveRouter

@AssistedFactory
interface AdaptiveContentStateFactory {
    fun create(
        scope: CoroutineScope,
        saveableStateHolder: SaveableStateHolder?,
    ): SavedStateAdaptiveContentState
}

private class InnerAdaptiveHostRouter(
    navStateFlow: StateFlow<MultiStackNav>,
    val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveConfiguration<ThreePane, Route>>,
    val nodeViewModelFactoryProvider: NodeViewModelFactoryProvider,
) : NewAdaptiveRouter<ThreePane, Route> {

    var multiStackNav by mutableStateOf(navStateFlow.value)

    private val configurationTrie = RouteTrie<AdaptiveConfiguration<ThreePane, Route>>().apply {
        routeConfigurationMap
            .mapKeys { (template) -> PathPattern(template) }
            .forEach(::set)
    }

    override val navigationState: Node
        get() = multiStackNav

    override val currentNode: Route
        get() = multiStackNav.current as? Route ?: unknownRoute("")

    override fun configuration(
        node: Route
    ): AdaptiveConfiguration<ThreePane, Route> {
        val configuration = configurationTrie[node]!!
        return adaptiveConfiguration(
            transitions = configuration.transitions,
            paneMapping = configuration.paneMapper,
            render = { paneNode ->
                val factory =
                    remember(paneNode) { nodeViewModelFactoryProvider.viewModelFactoryFor(paneNode) }
                CompositionLocalProvider(
                    LocalViewModelFactory provides factory
                ) {
                    with(configuration) {
                        render(paneNode)
                    }
                }
            }
        )
    }
}

@Stable
class SavedStateAdaptiveContentState @AssistedInject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveConfiguration<ThreePane, Route>>,
    private val nodeViewModelFactoryProvider: NodeViewModelFactoryProvider,
    private val navStateFlow: StateFlow<MultiStackNav>,
    private val uiStateFlow: StateFlow<UiState>,
    @Assisted coroutineScope: CoroutineScope,
    @Assisted saveableStateHolder: SaveableStateHolder,
) : AdaptiveContentState, SaveableStateHolder by saveableStateHolder {

    var windowSizeClass = mutableStateOf(WindowSizeClass.COMPACT)

    @Composable
    override fun scope(): AdaptiveHostScope<ThreePane, Route> {
        val inner = remember {
            InnerAdaptiveHostRouter(
                navStateFlow = navStateFlow,
                routeConfigurationMap = routeConfigurationMap,
                nodeViewModelFactoryProvider = nodeViewModelFactoryProvider
            )
        }
        val actual = remember {
            SavedStateAdaptiveNavHostState(
                panes = ThreePane.entries.toList(),
                adaptiveRouter = inner
                    .adaptFor(windowSizeClass),
                saveableStateHolder = this,
            )
        }

        val nav = remember { navStateFlow }
        val ui = remember { uiStateFlow }

        LaunchedEffect(nav, inner) {
            nav.collect { inner.multiStackNav = it }
        }

        LaunchedEffect(ui) {
            ui.collect {
                windowSizeClass.value = it.windowSizeClass
            }
        }

        return actual.scope()
    }

    override val overlays: Collection<SharedElementOverlay>
        get() = keysToMovableSharedElements.values

    private val keysToMovableSharedElements = mutableStateMapOf<Any, MovableSharedElementData<*>>()


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


//@Composable
//private fun AnimatedVisibilityScope.modifierFor(
//    adaptiveRouter: AdaptiveRouter,
//    paneState: Adaptive.PaneState,
//    windowSizeClass: WindowSizeClass,
//): Modifier = when (paneState.pane) {
//    Adaptive.Pane.Primary, Adaptive.Pane.Secondary -> FillSizeModifier
//        .background(color = MaterialTheme.colorScheme.surface)
//        .then(
//            when {
//                windowSizeClass.minWidthDp > WindowSizeClass.COMPACT.minWidthDp -> Modifier.clip(
//                    RoundedCornerShape(16.dp)
//                )
//
//                else -> Modifier
//            }
//        )
//        .then(
//            when (val enterAndExit = adaptiveRouter.transitionsFor(paneState)) {
//                null -> Modifier
//                else -> Modifier.animateEnterExit(
//                    enter = enterAndExit.enter,
//                    exit = enterAndExit.exit
//                )
//            }
//        )
//
//    Adaptive.Pane.TransientPrimary -> FillSizeModifier
//        .backPreviewModifier()
//
//    null -> FillSizeModifier
//}
//
//private val FillSizeModifier = Modifier.fillMaxSize()