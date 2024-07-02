package com.tunjid.trips.di

import androidx.compose.ui.Modifier
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.viewModel
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import com.tunjid.trips.State
import com.tunjid.trips.TripsScreen
import com.tunjid.trips.TripsViewModel
import com.tunjid.trips.TripsViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.modules.subclass

private const val RoutePattern = "/trips"

@Module
@InstallIn(SingletonComponent::class)
object TripsModule {

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
    fun routeAdaptiveConfiguration() = adaptiveRouteConfiguration { route ->
        val viewModel = viewModel<TripsViewModel>()
        TripsScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
        )
    }

    @IntoMap
    @Provides
    @ClassKey(TripsViewModel::class)
    fun tripsStateHolderCreator(
        factory: TripsViewModelFactory
    ): ScreenStateHolderCreator = factory
}