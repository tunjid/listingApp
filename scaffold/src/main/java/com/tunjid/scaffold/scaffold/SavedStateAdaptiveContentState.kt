package com.tunjid.scaffold.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.adaptive.MovableSharedElementData
import com.tunjid.scaffold.adaptive.SharedElementOverlay
import com.tunjid.scaffold.globalui.BackStatus
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.lifecycle.LocalViewModelFactory
import com.tunjid.scaffold.lifecycle.NodeViewModelFactoryProvider
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNavHostScope
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.adaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.adaptive.threepane.windowSizeClassConfiguration
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class SavedStateAdaptiveContentState @Inject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveNodeConfiguration<ThreePane, Route>>,
    private val nodeViewModelFactoryProvider: NodeViewModelFactoryProvider,
    private val navStateFlow: StateFlow<MultiStackNav>,
    private val uiStateFlow: StateFlow<UiState>,
) : AdaptiveContentState {

    private var windowSizeClass = mutableStateOf(WindowSizeClass.COMPACT)
    private var backStatus = mutableStateOf<BackStatus>(BackStatus.None)

    @Composable
    override fun scope(): AdaptiveNavHostScope<ThreePane, Route> {

        val multiStackNavState = remember {
            mutableStateOf(navStateFlow.value)
        }

        val configurationTrie = remember {
            RouteTrie<AdaptiveNodeConfiguration<ThreePane, Route>>().apply {
                routeConfigurationMap
                    .mapKeys { (template) -> PathPattern(template) }
                    .forEach(::set)
            }
        }

        val adaptiveNavHostConfiguration = remember {
            adaptiveNavHostConfiguration(
                navigationState = multiStackNavState,
                currentNode = derivedStateOf {
                    multiStackNavState.value.current as? Route ?: unknownRoute("")
                },
                configuration = { node ->
                    val configuration = configurationTrie[node]!!
                    adaptiveNodeConfiguration(
                        transitions = configuration.transitions,
                        paneMapping = configuration.paneMapper,
                        render = { paneNode ->
                            val factory = remember(paneNode) {
                                nodeViewModelFactoryProvider.viewModelFactoryFor(paneNode)
                            }
                            CompositionLocalProvider(
                                LocalViewModelFactory provides factory
                            ) {
                                with(configuration) { render(paneNode) }
                            }
                        }
                    )
                }
            )
        }
        val adaptiveNavHostState = remember {
            SavedStateAdaptiveNavHostState<ThreePane, Route>(
                panes = ThreePane.entries.toList(),
                router = adaptiveNavHostConfiguration
                    .windowSizeClassConfiguration(
                        windowSizeClassState = windowSizeClass
                    )
                    .backPreviewConfiguration(
                        windowSizeClassState = windowSizeClass,
                        backStatusState = backStatus
                    ),
            )
        }

        val rememberedNavStateFlow = remember { navStateFlow }
        val rememberedUiStateFlow = remember { uiStateFlow }

        LaunchedEffect(rememberedNavStateFlow, adaptiveNavHostConfiguration) {
            rememberedNavStateFlow.collect {
                multiStackNavState.value = it
            }
        }

        LaunchedEffect(rememberedUiStateFlow) {
            rememberedUiStateFlow.collect {
                windowSizeClass.value = it.windowSizeClass
                backStatus.value = it.backStatus
            }
        }

        return adaptiveNavHostState.scope()
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


private fun AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, Route>.backPreviewConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
    backStatusState: State<BackStatus>,
) = delegated { node ->
    val original = this@backPreviewConfiguration.configuration(node)
    AdaptiveNodeConfiguration(
        transitions = original.transitions,
        paneMapper = paneMapper@{ inner ->
            val originalMapping = original.paneMapper(inner)
            val previousRoute =
                navigationState.value.pop().current as? Route
                    ?: return@paneMapper originalMapping

            // Consider navigation state different if window size class changes
            val backStatus by backStatusState
            val isPreviewingBack = backStatus.isPreviewing
                    && previousRoute.id != originalMapping[ThreePane.Primary]?.id
                    && previousRoute.id != originalMapping[ThreePane.Secondary]?.id

            if (!isPreviewingBack) return@paneMapper originalMapping

            original.paneMapper(previousRoute) +
                    (ThreePane.TransientPrimary to originalMapping[ThreePane.Primary])
        },
        render = paneScope@{ toRender ->
            val windowSizeClass by windowSizeClassState
            Box(
                Modifier.modifierFor(
                    windowSizeClass = windowSizeClass,
                    nodeConfiguration = original,
                    adaptivePaneScope = this@paneScope
                )
            )
            {
                original.render.invoke(this@paneScope, toRender)
            }
        }
    )
}

@Composable
private fun Modifier.modifierFor(
    windowSizeClass: WindowSizeClass,
    nodeConfiguration: AdaptiveNodeConfiguration<ThreePane, Route>,
    adaptivePaneScope: AdaptivePaneScope<ThreePane, Route>,
): Modifier = this then with(adaptivePaneScope) {
    when (paneState.pane) {
        ThreePane.Primary, ThreePane.Secondary -> FillSizeModifier
            .background(color = MaterialTheme.colorScheme.surface)
            .run {
                when {
                    windowSizeClass.minWidthDp > WindowSizeClass.COMPACT.minWidthDp -> Modifier.clip(
                        RoundedCornerShape(16.dp)
                    )

                    else -> Modifier
                }
            }
            .run {
                val enterAndExit = nodeConfiguration.transitions(adaptivePaneScope)
                Modifier.animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            }

        ThreePane.TransientPrimary -> FillSizeModifier
            .backPreviewModifier()

        else -> FillSizeModifier
    }
}

private val FillSizeModifier = Modifier.fillMaxSize()