package com.tunjid.explore.grid.di

import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.explore.grid.ExploreGridModelFactory
import com.tunjid.explore.grid.ExploreGridScreen
import com.tunjid.explore.grid.ExploreGridViewModel
import com.tunjid.explore.grid.State
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.viewModelCoroutineScope
import com.tunjid.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.movableSharedElementScope
import com.tunjid.treenav.adaptive.threepane.threePaneAdaptiveNodeConfiguration
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.modules.subclass

private const val RoutePattern = "/explore"

@Module
@InstallIn(SingletonComponent::class)
object ExploreGridModule {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::routeOf
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeAdaptiveConfiguration(
        factory: ExploreGridModelFactory
    ) = threePaneAdaptiveNodeConfiguration { route ->
        val viewModel = viewModel<ExploreGridViewModel> {
            factory.create(
                scope = viewModelCoroutineScope(),
                route = route,
            )
        }
        ScreenUiState(
            UiState(
                fabShows = false,
                navVisibility = NavVisibility.Visible,
                insetFlags = InsetFlags.NONE
            )
        )
        ExploreGridScreen(
            movableSharedElementScope = movableSharedElementScope(),
            modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
        )
    }
}