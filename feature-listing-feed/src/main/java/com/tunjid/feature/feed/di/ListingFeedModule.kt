package com.tunjid.feature.feed.di

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.feed.ListingFeedScreen
import com.tunjid.feature.feed.ListingFeedViewModelFactory
import com.tunjid.feature.feed.ListingFeedViewModel
import com.tunjid.listing.data.model.ListingQuery
import com.tunjid.listing.feature.listing.feed.R
import com.tunjid.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.di.AssistedViewModelFactory
import com.tunjid.scaffold.scaffold.PaneNavigationBar
import com.tunjid.scaffold.scaffold.PaneNavigationRail
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.bottomNavigationNestedScrollConnection
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.mappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

internal const val FeedPattern = "/listings"
internal const val FavoritesPattern = "/favorites"
private const val DefaultItemsPerQuery = 10L

internal val Route.limit by mappedRouteQuery(
    default = DefaultItemsPerQuery,
    mapper = String::toLong,
)

internal val Route.offset by mappedRouteQuery(
    default = 0L,
    mapper = String::toLong,
)

internal val Route.propertyType by optionalRouteQuery()

internal val Route.isFavorites get() = routeParams.pathAndQueries.contains("favorites")

internal val Route.startingMediaUrls get() = routeParams.queryParams["url"]?.take(3) ?: emptyList()

internal val Route.initialQuery
    get() = ListingQuery(
        propertyType = propertyType,
        limit = limit,
        offset = offset,
    )

@Module
@InstallIn(SingletonComponent::class)
object ListingFeedModule {

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun feedParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = FeedPattern,
            routeMapper = ::routeOf
        )

    @IntoMap
    @Provides
    @StringKey(FavoritesPattern)
    fun favoritesParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = FavoritesPattern,
            routeMapper = ::routeOf
        )

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun feedNavEntry(
        factory: ListingFeedViewModelFactory
    ) = threePaneEntry { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ListingFeedViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }

        val bottomNavigationOffsetConnection =
            bottomNavigationNestedScrollConnection()

        rememberPaneScaffoldState().PaneScaffold(
            modifier = Modifier
                .predictiveBackBackgroundModifier(paneScope = this)
                .nestedScroll(bottomNavigationOffsetConnection),
            showNavigation = false,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.listing_app))
                    },
                )
            },
            content = { paddingValues ->
                ListingFeedScreen(
                    scaffoldState = this,
                    modifier = Modifier
                        .padding(
                            top = paddingValues.calculateTopPadding()
                        ),
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    actions = viewModel.accept
                )
            },
            navigationBar = {
                PaneNavigationBar(
                    modifier = Modifier
                        .offset {
                            bottomNavigationOffsetConnection.offset.round()
                        }
                        .animateEnterExit(
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                        )
                )
            },
            navigationRail = {
                if (paneState.pane == ThreePane.Primary) PaneNavigationRail()
            }
        )
    }

    @IntoMap
    @Provides
    @StringKey(FavoritesPattern)
    fun favoritesNavEntry(
        factory: ListingFeedViewModelFactory
    ) = feedNavEntry(factory)

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun listingFeedStateHolderCreator(
        factory: ListingFeedViewModelFactory
    ): AssistedViewModelFactory = factory
}