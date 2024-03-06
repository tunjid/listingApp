package com.tunjid.feature.feed.di

import androidx.compose.ui.Modifier
import com.tunjid.feature.feed.ListingFeedScreen
import com.tunjid.feature.feed.ListingFeedStateHolder
import com.tunjid.feature.feed.ListingFeedStateHolderFactory
import com.tunjid.feature.feed.State
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.rememberRetainedStateHolder
import com.tunjid.scaffold.navigation.SerializedRouteParams
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.subclass

private const val FeedPattern = "/listings"
private const val FavoritesPattern = "/favorites"
private const val DefaultItemsPerQuery = 10L

@Serializable
internal data class ListingFeedRoute(
    override val routeParams: SerializedRouteParams
) : Route

internal val RouteParams.limit
    get() = queryParams["limit"]?.firstOrNull()?.toLongOrNull()
        ?: DefaultItemsPerQuery

internal val RouteParams.offset get() = queryParams["offset"]?.firstOrNull()?.toLongOrNull() ?: 0L

internal val RouteParams.propertyType get() = queryParams["propertyType"]?.firstOrNull()

internal val RouteParams.isFavorites get() = pathAndQueries.contains("favorites")

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
            routeMapper = ::ListingFeedRoute
        )

    @IntoMap
    @Provides
    @StringKey(FavoritesPattern)
    fun favoritesParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = FavoritesPattern,
            routeMapper = ::ListingFeedRoute
        )

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun feedAdaptiveConfiguration() = adaptiveRouteConfiguration { route ->
        val stateHolder = rememberRetainedStateHolder<ListingFeedStateHolder>(
            route = route
        )
        ListingFeedScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }

    @IntoMap
    @Provides
    @StringKey(FavoritesPattern)
    fun favoritesAdaptiveConfiguration() = feedAdaptiveConfiguration()

    @IntoMap
    @Provides
    @StringKey(FeedPattern)
    fun listingFeedStateHolderCreator(
        factory: ListingFeedStateHolderFactory
    ): ScreenStateHolderCreator = factory::create

    @IntoMap
    @Provides
    @StringKey(FavoritesPattern)
    fun favoritesStateHolderCreator(
        factory: ListingFeedStateHolderFactory
    ): ScreenStateHolderCreator = factory::create
}