package com.tunjid.feature.listinggallery.grid.di

import androidx.compose.ui.Modifier
import com.tunjid.feature.listinggallery.grid.GridGalleryScreen
import com.tunjid.feature.listinggallery.grid.GridGalleryStateHolder
import com.tunjid.feature.listinggallery.grid.GridGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.grid.State
import com.tunjid.listing.data.model.MediaQuery
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
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.subclass

private const val RoutePattern = "/listings/{listingId}/gallery/grid"

@Serializable
data class GridGalleryRoute(
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
object GridGalleryModule {

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
            routeMapper = ::GridGalleryRoute
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeAdaptiveConfiguration() = adaptiveRouteConfiguration { route ->
        val stateHolder = rememberRetainedStateHolder<GridGalleryStateHolder>(
            route = route
        )
        GridGalleryScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun listingGalleryStateHolderCreator(
        factory: GridGalleryStateHolderFactory
    ): ScreenStateHolderCreator = factory::create
}