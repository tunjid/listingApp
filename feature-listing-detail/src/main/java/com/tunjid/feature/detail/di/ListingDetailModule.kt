package com.tunjid.feature.detail.di

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.detail.Action
import com.tunjid.feature.detail.ListingDetailScreen
import com.tunjid.feature.detail.ListingDetailViewModel
import com.tunjid.feature.detail.ListingViewModelFactory
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.feature.listing.detail.R
import com.tunjid.me.scaffold.scaffold.PaneFab
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.scaffold.PaneNavigationRail
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.scaffold.scaffold.rememberPaneScaffoldState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routePath
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

private const val RoutePattern = "/listings/{listingId}"

private fun listingDetailRoute(
    routeParams: RouteParams
) = routeOf(
    params = routeParams,
    children = listOf(
        routeOf("/listings")
    )
)

internal val Route.listingId by routePath()

internal val Route.startingMediaUrls get() = routeParams.queryParams["url"] ?: emptyList()

internal val Route.initialQuery
    get() = MediaQuery(
        listingId = listingId,
        offset = routeParams.queryParams["offset"]?.first()?.toLongOrNull() ?: 0L,
        limit = routeParams.queryParams["limit"]?.first()?.toLongOrNull() ?: 4L,
    )

@Module
@InstallIn(SingletonComponent::class)
object ListingDetailModule {

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeMatcher(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::listingDetailRoute
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeNavEntry(
        factory: ListingViewModelFactory
    ) = threePaneEntry<Route>(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.first() as? Route
            )
        },
        render = { route ->
            val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
            val viewModel = viewModel<ListingDetailViewModel> {
                factory.create(
                    scope = lifecycleCoroutineScope,
                    route = route,
                )
            }
            val state = viewModel.state.collectAsStateWithLifecycle().value
            rememberPaneScaffoldState().PaneScaffold(
                modifier = Modifier
                    .predictiveBackBackgroundModifier(paneScope = this),
                showNavigation = false,
                topBar = {
                    PoppableDestinationTopAppBar(
                        onBackPressed = {
                            viewModel.accept(Action.Navigation.Pop())
                        }
                    )
                },
                content = {
                    ListingDetailScreen(
                        scaffoldState = this,
                        modifier = Modifier,
                        state = state,
                        actions = viewModel.accept
                    )
                    // Close the secondary pane when invoking back since it contains the list view
                    SecondaryPaneCloseBackHandler(
                        enabled = paneState.pane == ThreePane.Primary
                                && route.children.isNotEmpty()
                                && isMediumScreenWidthOrWider
                    )
                },
                navigationRail = {
                    PaneNavigationRail()
                },
                floatingActionButton = {
                    PaneFab(
                        modifier = Modifier
                            .animateEnterExit(
                                enter = slideInVertically(initialOffsetY = { it }),
                                exit = slideOutVertically(targetOffsetY = { it }),
                            ),
                        text = stringResource(R.string.book),
                        icon = Icons.Rounded.Sell,
                        expanded = true,
                        onClick = {

                        }
                    )
                }
            )
        }
    )
}