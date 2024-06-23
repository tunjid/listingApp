package com.tunjid.feature.detail.di

import androidx.compose.ui.Modifier
import com.tunjid.feature.detail.ListingDetailScreen
import com.tunjid.feature.detail.ListingDetailViewModel
import com.tunjid.feature.detail.ListingStateHolderFactory
import com.tunjid.feature.detail.State
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.viewModel
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.strings.Route
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

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

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
    fun routeAdaptiveConfiguration() = adaptiveRouteConfiguration(
        secondaryRoute = { route ->
            route.children.first() as? Route
        },
        render = {
            val viewModel = viewModel<ListingDetailViewModel>()
            ListingDetailScreen(
                modifier = Modifier.backPreviewBackgroundModifier(),
                state = viewModel.state.collectAsStateWithLifecycle().value,
                actions = viewModel.accept
            )
        })

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun archiveListStateHolderCreator(
        factory: ListingStateHolderFactory
    ): ScreenStateHolderCreator = factory
}