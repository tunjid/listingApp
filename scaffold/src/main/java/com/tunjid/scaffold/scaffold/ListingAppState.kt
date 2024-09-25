package com.tunjid.scaffold.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.windowSizeClassConfiguration
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.adaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.paneMapping
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class ListingAppState @Inject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveNodeConfiguration<ThreePane, Route>>,
    private val navStateFlow: StateFlow<MultiStackNav>,
    private val uiStateFlow: StateFlow<UiState>,
) {

    private val multiStackNavState = mutableStateOf(navStateFlow.value)
    private val windowSizeClass = mutableStateOf(WindowSizeClass.COMPACT)
    private val isPreviewing = mutableStateOf(false)

    val configurationTrie = RouteTrie<AdaptiveNodeConfiguration<ThreePane, Route>>().apply {
        routeConfigurationMap
            .mapKeys { (template) -> PathPattern(template) }
            .forEach(::set)
    }

    private val adaptiveNavHostConfiguration = adaptiveNavHostConfiguration(
        navigationState = multiStackNavState,
        currentNode = derivedStateOf {
            multiStackNavState.value.current as? Route ?: unknownRoute("")
        },
        configuration = { node ->
            configurationTrie[node]!!
        }
    )

    fun adaptiveNavHostState(
        configurationBlock: AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, Route>.() -> AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, Route>
    ) = SavedStateAdaptiveNavHostState(
        panes = ThreePane.entries.toList(),
        configuration = adaptiveNavHostConfiguration
            .windowSizeClassConfiguration(
                windowSizeClassState = windowSizeClass
            )
            .backPreviewConfiguration(
                windowSizeClassState = windowSizeClass,
                isPreviewingState = isPreviewing
            ).configurationBlock(),
    )

    suspend fun start() = coroutineScope {
        awaitAll(
            async {
                navStateFlow.collect {
                    multiStackNavState.value = it
                }
            },
            async {
                uiStateFlow.distinctUntilChangedBy {
                    it.windowSizeClass to it.backStatus.isPreviewing
                }
                    .collect {
                        windowSizeClass.value = it.windowSizeClass
                        isPreviewing.value = it.backStatus.isPreviewing
                    }
            }
        )
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
        adaptiveNodeConfiguration(
            transitions = originalConfiguration.transitions,
            paneMapping = paneMapper@{ inner ->
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
                    Modifier.adaptiveModifier(
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
private fun Modifier.adaptiveModifier(
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