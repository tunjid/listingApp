package com.tunjid.scaffold.scaffold

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.navigation.NavItem
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.navItemSelected
import com.tunjid.scaffold.navigation.navItems
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
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class ListingAppState @Inject constructor(
    private val routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveNodeConfiguration<ThreePane, Route>>,
    private val navigationStateHolder: NavigationStateHolder,
    private val globalUiStateHolder: GlobalUiStateHolder
) {

    private val multiStackNavState = mutableStateOf(navigationStateHolder.state.value)
    private val uiState = mutableStateOf(globalUiStateHolder.state.value)

    val navItems by derivedStateOf { multiStackNavState.value.navItems }
    val globalUi by uiState

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
                    backStatusState = derivedStateOf {
                        uiState.value.backStatus
                    },
                )
                .configurationBlock(),
        )
    }

    fun updateGlobalUi(
        block: UiState.() -> UiState
    ) {
        globalUiStateHolder.accept(block)
    }

    fun onNavItemSelected(navItem: NavItem) {
        navigationStateHolder.accept { navState.navItemSelected(item = navItem) }
    }

    suspend fun start() {
        combine(
            navigationStateHolder.state,
            globalUiStateHolder.state,
            ::Pair,
        ).collect { (multiStackNav, ui) ->
            uiState.value = ui
            multiStackNavState.value = multiStackNav
        }
    }
}