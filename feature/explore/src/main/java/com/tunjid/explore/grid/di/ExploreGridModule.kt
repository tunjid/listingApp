package com.tunjid.explore.grid.di

import androidx.compose.ui.Modifier
import com.tunjid.explore.grid.ExploreGridModelFactory
import com.tunjid.explore.grid.ExploreGridScreen
import com.tunjid.explore.grid.ExploreGridViewModel
import com.tunjid.explore.grid.State
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.viewModel
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.adaptive.threepane.threePaneAdaptiveNodeConfiguration
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
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
    fun routeAdaptiveConfiguration() = threePaneAdaptiveNodeConfiguration<Route> {
        val viewModel = viewModel<ExploreGridViewModel>()
        ExploreGridScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
        )
    }

    @IntoMap
    @Provides
    @ClassKey(ExploreGridViewModel::class)
    fun tripsStateHolderCreator(
        factory: ExploreGridModelFactory
    ): ScreenStateHolderCreator = factory
}