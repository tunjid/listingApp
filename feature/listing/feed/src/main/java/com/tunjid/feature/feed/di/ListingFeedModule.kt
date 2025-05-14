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
import com.tunjid.feature.feed.ListingFeedStateHolderFactory
import com.tunjid.feature.feed.ListingFeedViewModel
import com.tunjid.listing.data.model.ListingQuery
import com.tunjid.listing.feature.listing.feed.R
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.scaffold.PaneBottomAppBar
import com.tunjid.scaffold.scaffold.PaneNavigationRail
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.bottomNavigationNestedScrollConnection
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.ThreePane
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

internal const val FeedPattern = "/listings"
internal const val FavoritesPattern = "/favorites"
private const val DefaultItemsPerQuery = 10L

internal val RouteParams.limit
    get() = queryParams["limit"]?.firstOrNull()?.toLongOrNull()
        ?: DefaultItemsPerQuery

internal val RouteParams.offset get() = queryParams["offset"]?.firstOrNull()?.toLongOrNull() ?: 0L

internal val RouteParams.propertyType get() = queryParams["propertyType"]?.firstOrNull()

internal val RouteParams.isFavorites get() = pathAndQueries.contains("favorites")

internal val RouteParams.startingMediaUrls get() = queryParams["url"]?.take(3) ?: emptyList()

internal val RouteParams.initialQuery
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
    fun feedAdaptiveConfiguration(
        factory: ListingFeedStateHolderFactory
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
                PaneBottomAppBar(
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
    fun favoritesAdaptiveConfiguration(
        factory: ListingFeedStateHolderFactory
    ) = feedAdaptiveConfiguration(factory)

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun listingFeedStateHolderCreator(
        factory: ListingFeedStateHolderFactory
    ): ScreenStateHolderCreator = factory
}