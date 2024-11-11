package com.tunjid.explore.pager.di

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.explore.pager.ExplorePagerStateHolderFactory
import com.tunjid.explore.pager.ExplorePagerViewModel
import com.tunjid.explore.pager.FullscreenGalleryScreen
import com.tunjid.explore.pager.State
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.treenav.compose.threepane.configurations.movableSharedElementScope
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.modules.subclass

private const val RoutePattern = "/explore/pager"

internal val RouteParams.startingUrls get() = queryParams["url"] ?: emptyList()
internal val RouteParams.initialPage
    get() = queryParams["startingUrl"]
        ?.firstOrNull()
        ?.let(startingUrls::indexOf)
        ?: 0


@Module
@InstallIn(SingletonComponent::class)
object ExplorePagerModule {

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
        factory: ExplorePagerStateHolderFactory
    ) = threePaneListDetailStrategy { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ExplorePagerViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }
        ScreenUiState(
            UiState(
                fabShows = false,
                navVisibility = NavVisibility.Gone,
                insetFlags = InsetFlags.NONE
            )
        )
        FullscreenGalleryScreen(
            movableSharedElementScope = movableSharedElementScope(),
            modifier = Modifier,
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
        )
    }
}