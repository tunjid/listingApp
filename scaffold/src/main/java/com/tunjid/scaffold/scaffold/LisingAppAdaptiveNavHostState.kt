package com.tunjid.scaffold.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNavHostScope
import com.tunjid.treenav.adaptive.AdaptiveNavHostState
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.adaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.paneMapping
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
class LisingAppAdaptiveNavHostState @Inject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveNodeConfiguration<ThreePane, Route>>,
    private val navStateFlow: StateFlow<MultiStackNav>,
    private val uiStateFlow: StateFlow<UiState>,
) : AdaptiveNavHostState<ThreePane, Route> {

    private var windowSizeClass = mutableStateOf(WindowSizeClass.COMPACT)
    private var isPreviewing = mutableStateOf(false)

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
                            with(configuration) { render(paneNode) }
                        }
                    )
                }
            )
        }
        val adaptiveNavHostState = remember {
            SavedStateAdaptiveNavHostState(
                panes = ThreePane.entries.toList(),
                configuration = adaptiveNavHostConfiguration
                    .windowSizeClassConfiguration(
                        windowSizeClassState = windowSizeClass
                    )
                    .backPreviewConfiguration(
                        windowSizeClassState = windowSizeClass,
                        isPreviewingState = isPreviewing
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
                isPreviewing.value = it.backStatus.isPreviewing
            }
        }

        return adaptiveNavHostState.scope()
    }
}


private fun AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, Route>.backPreviewConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
    isPreviewingState: State<Boolean>,
) = delegated(
    currentNode = derivedStateOf {
        val current = currentNode.value
        if (isPreviewingState.value) navigationState.value.pop().current as Route
        else current
    },
    configuration = { node ->
        val originalConfiguration = configuration(node)
        AdaptiveNodeConfiguration(
            transitions = originalConfiguration.transitions,
            paneMapper = paneMapper@{ inner ->
                val originalMapping = originalConfiguration.paneMapper(inner)

                val isPreviewingBack by isPreviewingState
                if (!isPreviewingBack) return@paneMapper originalMapping

                // Back is being previewed, therefore the original mapping is already for back.
                // Pass the previous primary value into transient.
                val transient = this@backPreviewConfiguration.paneMapping()[ThreePane.Primary]
                originalMapping + (ThreePane.TransientPrimary to transient)
            },
            render = paneScope@{ toRender ->
                val windowSizeClass by windowSizeClassState
                Box(
                    Modifier.modifierFor(
                        windowSizeClass = windowSizeClass,
                        nodeConfiguration = originalConfiguration,
                        adaptivePaneScope = this@paneScope
                    )
                )
                {
                    originalConfiguration.render.invoke(this@paneScope, toRender)
                }
            }
        )
    })

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