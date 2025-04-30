package com.tunjid.feature.detail.di

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.detail.Action
import com.tunjid.feature.detail.ListingDetailScreen
import com.tunjid.feature.detail.ListingDetailViewModel
import com.tunjid.feature.detail.ListingStateHolderFactory
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.me.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.me.scaffold.scaffold.predictiveBackBackgroundModifier
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.scaffold.PaneScaffold
import com.tunjid.scaffold.scaffold.PoppableDestinationTopAppBar
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParams
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

internal val RouteParams.listingId get() = pathArgs.getValue("listingId")

internal val RouteParams.startingMediaUrls get() = queryParams["url"] ?: emptyList()

internal val RouteParams.initialQuery
    get() = MediaQuery(
        listingId = listingId,
        offset = queryParams["pageOffset"]?.first()?.toLongOrNull() ?: 0L,
        limit = queryParams["pageLimit"]?.first()?.toLongOrNull() ?: 4L,
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
    fun routeAdaptiveConfiguration(
        factory: ListingStateHolderFactory
    ) = threePaneEntry(
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
            PaneScaffold(
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
                        movableSharedElementScope = this,
                        modifier = Modifier,
                        state = state,
                        actions = viewModel.accept
                    )
                },
            )
            // Close the secondary pane when invoking back since it contains the list view
            SecondaryPaneCloseBackHandler(
                enabled = state.isInPrimaryNav && state.hasSecondaryPanel
            )
        }
    )
}