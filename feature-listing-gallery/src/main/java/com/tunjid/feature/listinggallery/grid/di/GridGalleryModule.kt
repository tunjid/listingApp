package com.tunjid.feature.listinggallery.grid.di

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
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
import com.tunjid.me.scaffold.scaffold.PaneFab
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

private const val RoutePattern = "/listings/{listingId}/gallery/grid"

internal val Route.listingId by routePath()

internal val Route.startingMediaUrls get() = routeParams.queryParams["url"] ?: emptyList()

internal val Route.initialQuery
    get() = MediaQuery(
        listingId = listingId,
        offset = routeParams.queryParams["pageOffset"]?.first()?.toLongOrNull() ?: 0L,
        limit = routeParams.queryParams["pageLimit"]?.first()?.toLongOrNull() ?: 12L,
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
        rememberPaneScaffoldState().PaneScaffold(
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
            content = { paddingValues ->
                GridGalleryScreen(
                    paneScaffoldState = this,
                    modifier = Modifier
                        .padding(
                            top = paddingValues.calculateTopPadding()
                        ),
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },
            floatingActionButton = {
                PaneFab(
                    modifier = Modifier
                        .animateEnterExit(
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        ),
                    text = stringResource(R.string.favorite),
                    icon = Icons.Rounded.Favorite,
                    expanded = true,
                    onClick = {

                    }
                )
            }
        )
    }
}