package com.tunjid.feature.feed.di

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.feed.ListingFeedScreen
import com.tunjid.feature.feed.ListingFeedStateHolderFactory
import com.tunjid.feature.feed.ListingFeedViewModel
import com.tunjid.feature.feed.State
import com.tunjid.listing.data.model.ListingQuery
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.configurations.requireThreePaneMovableSharedElementScope
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

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

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
    ) = threePaneListDetailStrategy { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<ListingFeedViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }
        ScreenUiState(
            UiState(
                fabShows = false,
                navVisibility = NavVisibility.Visible,
                insetFlags = InsetFlags.NONE
            )
        )
        ListingFeedScreen(
            movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
            modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
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