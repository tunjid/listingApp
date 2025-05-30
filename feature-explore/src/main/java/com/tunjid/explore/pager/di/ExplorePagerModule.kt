package com.tunjid.explore.pager.di

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.explore.pager.ExplorePagerViewModelFactory
import com.tunjid.explore.pager.ExplorePagerViewModel
import com.tunjid.explore.pager.FullscreenGalleryScreen
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

private const val RoutePattern = "/explore/pager"

internal val Route.startingUrls get() = routeParams.queryParams["url"] ?: emptyList()
internal val Route.initialPage
    get() = routeParams
        .queryParams["startingUrl"]
        ?.firstOrNull()
        ?.let(startingUrls::indexOf)
        ?: 0


@Module
@InstallIn(SingletonComponent::class)
object ExplorePagerModule {

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
    fun routeNavEntry(
        factory: ExplorePagerViewModelFactory
    ) = threePaneEntry { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ExplorePagerViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }
        rememberPaneScaffoldState().PaneScaffold(
            modifier = Modifier,
            showNavigation = false,
            containerColor = Color.Transparent,
            content = {
                FullscreenGalleryScreen(
                    scaffoldState = this,
                    modifier = Modifier,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },
        )
    }
}