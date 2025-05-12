package com.tunjid.feature.listinggallery.pager.di

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.listinggallery.pager.FullscreenGalleryScreen
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.pager.PagerGalleryViewModel
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

private const val RoutePattern = "/listings/{listingId}/gallery/pager"

internal val RouteParams.listingId get() = pathArgs.getValue("listingId")

internal val RouteParams.startingMediaUrls get() = queryParams["url"] ?: emptyList()

internal val RouteParams.initialQuery
    get() = MediaQuery(
        listingId = listingId,
        offset = queryParams["pageOffset"]?.first()?.toLongOrNull() ?: 0L,
        limit = queryParams["pageLimit"]?.first()?.toLongOrNull() ?: 12L,
    )

@Module
@InstallIn(SingletonComponent::class)
object PagerGalleryModule {

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
        factory: PagerGalleryStateHolderFactory
    ) = threePaneEntry { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<PagerGalleryViewModel> {
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
                    movableSharedElementScope = this,
                    modifier = Modifier,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },
        )
    }
}