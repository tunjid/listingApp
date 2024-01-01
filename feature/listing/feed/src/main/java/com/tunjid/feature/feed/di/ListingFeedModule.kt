package com.tunjid.feature.feed.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.feature.feed.ListingFeedScreen
import com.tunjid.feature.feed.ListingFeedStateHolder
import com.tunjid.feature.feed.ListingFeedStateHolderFactory
import com.tunjid.feature.feed.State
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.di.downcast
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.rememberRetainedStateHolder
import com.tunjid.scaffold.navigation.SerializedRouteParams
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.subclass

private const val ListingFeedRoutePattern = "listings"
private const val DefaultItemsPerQuery = 10L

@Serializable
data class ListingFeedRoute(
    override val routeParams: SerializedRouteParams
) : AdaptiveRoute {

    val limit
        get() = routeParams.queryParams["limit"]?.firstOrNull()?.toLongOrNull()
            ?: DefaultItemsPerQuery

    val offset get() = routeParams.queryParams["offset"]?.firstOrNull()?.toLongOrNull() ?: 0L

    val propertyType get() = routeParams.queryParams["propertyType"]?.firstOrNull()

    @Composable
    override fun Content() {
        val stateHolder = rememberRetainedStateHolder<ListingFeedStateHolder>(
            route = this@ListingFeedRoute
        )
        ListingFeedScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }
}

@Composable
private fun ListingFeedRoute(route: ListingFeedRoute) {
    val stateHolder = rememberRetainedStateHolder<ListingFeedStateHolder>(
        route = route
    )
    ListingFeedScreen(
        modifier = Modifier.backPreviewBackgroundModifier(),
        state = stateHolder.state.collectAsStateWithLifecycle().value,
        actions = stateHolder.accept
    )
}


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
    @StringKey(ListingFeedRoutePattern)
    fun listingFeedRouteParser(): UrlRouteMatcher<AdaptiveRoute> =
        urlRouteMatcher(
            routePattern = ListingFeedRoutePattern,
            routeMapper = ::ListingFeedRoute
        )

    @IntoMap
    @Provides
    @ClassKey(ListingFeedRoute::class)
    fun listingFeedStateHolderCreator(
        factory: ListingFeedStateHolderFactory
    ): ScreenStateHolderCreator = factory::create.downcast()
}