package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.navigation.unknownRoute
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.windowSizeClassConfiguration
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.SavedStateAdaptiveNavHostState
import com.tunjid.treenav.adaptive.adaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.current
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
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
    private val uiState = mutableStateOf(uiStateFlow.value)

    private val configurationTrie = RouteTrie<AdaptiveNodeConfiguration<ThreePane, Route>>().apply {
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
    ): SavedStateAdaptiveNavHostState<ThreePane, Route> {
        val windowSizeClassState = derivedStateOf { uiState.value.windowSizeClass }
        return SavedStateAdaptiveNavHostState(
            panes = ThreePane.entries.toList(),
            configuration = adaptiveNavHostConfiguration
                .windowSizeClassConfiguration(
                    windowSizeClassState = windowSizeClassState
                )
                .predictiveBackConfiguration(
                    windowSizeClassState = windowSizeClassState,
                    isPreviewingState = derivedStateOf {
                        uiState.value.backStatus.isPreviewing
                    }
                )
                .configurationBlock(),
        )
    }

    suspend fun start() = coroutineScope {
        awaitAll(
            async {
                navStateFlow.collect(multiStackNavState::value::set)
            },
            async {
                uiStateFlow.collect(uiState::value::set)
            },
        )
    }
}