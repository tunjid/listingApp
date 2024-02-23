package com.tunjid.feature.listinggallery.pager.di

import androidx.compose.ui.Modifier
import com.tunjid.feature.listinggallery.pager.FullscreenGalleryScreen
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolder
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.pager.State
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.adaptiveRouteConfiguration
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.rememberRetainedStateHolder
import com.tunjid.scaffold.navigation.SerializedRouteParams
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteMatcher
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

private const val RoutePattern = "/listings/{listingId}/gallery/pager"

@Serializable
internal data class PagerGalleryRoute(
    override val routeParams: SerializedRouteParams
) : Route

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
object PagerGalleryModule {

    @IntoSet
    @Provides
    fun savedStateType(): SavedStateType = SavedStateType {
        subclass(State::class)
    }

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeParser(): RouteMatcher =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::PagerGalleryRoute
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeAdaptiveConfiguration() = adaptiveRouteConfiguration { route ->
        val stateHolder = rememberRetainedStateHolder<PagerGalleryStateHolder>(
            route = route
        )
        FullscreenGalleryScreen(
            modifier = Modifier,
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun fullscreenGalleryStateHolderCreator(
        factory: PagerGalleryStateHolderFactory
    ): ScreenStateHolderCreator = factory::create
}