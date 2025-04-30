package com.tunjid.feature.listinggallery.grid.di

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.listinggallery.grid.Action
import com.tunjid.feature.listinggallery.grid.GridGalleryScreen
import com.tunjid.feature.listinggallery.grid.GridGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.grid.GridGalleryViewModel
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.feature.listing.gallery.R
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.PoppableDestinationTopAppBar
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

private const val RoutePattern = "/listings/{listingId}/gallery/grid"

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
object GridGalleryModule {

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
        factory: GridGalleryStateHolderFactory
    ) = threePaneEntry { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<GridGalleryViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }
        PaneScaffold(
            modifier = Modifier
                .predictiveBackBackgroundModifier(paneScope = this),
            showNavigation = true,
            topBar = {
                PoppableDestinationTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.gallery))
                    },
                    onBackPressed = {
                        viewModel.accept(Action.Navigation.Pop())
                    }
                )
            },
            content = {
                GridGalleryScreen(
                    movableSharedElementScope = this,
                    modifier = Modifier,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },

            navigationBar = {
                PaneBottomAppBar()
            },
        )
    }
}