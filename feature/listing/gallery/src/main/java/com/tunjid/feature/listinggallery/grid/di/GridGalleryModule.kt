package com.tunjid.feature.listinggallery.grid.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.feature.listinggallery.grid.GridGalleryScreen
import com.tunjid.feature.listinggallery.grid.GridGalleryStateHolder
import com.tunjid.feature.listinggallery.grid.GridGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.grid.State
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.di.downcast
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

private const val GridGalleryPattern = "listings/{listingId}/gallery/grid"

@Serializable
data class GridGalleryRoute(
    override val routeParams: SerializedRouteParams
) : AdaptiveRoute {

    val listingId get() = routeParams.pathArgs.getValue("listingId")

    val startingMediaUrls get() = routeParams.queryParams["url"] ?: emptyList()

    val initialQuery = ImageQuery(
        listingId = listingId,
        offset = routeParams.queryParams["pageOffset"]?.first()?.toLongOrNull() ?: 0L,
        limit = routeParams.queryParams["pageLimit"]?.first()?.toLongOrNull() ?: 4L,
    )

    @Composable
    override fun Content() {
        val stateHolder = rememberRetainedStateHolder<GridGalleryStateHolder>(
            route = this@GridGalleryRoute
        )
        GridGalleryScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }
}

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
    @StringKey(GridGalleryPattern)
    fun listingGalleryRouteParser(): UrlRouteMatcher<AdaptiveRoute> =
        urlRouteMatcher(
            routePattern = GridGalleryPattern,
            routeMapper = ::GridGalleryRoute
        )

    @IntoMap
    @Provides
    @ClassKey(GridGalleryRoute::class)
    fun listingGalleryStateHolderCreator(
        factory: GridGalleryStateHolderFactory
    ): ScreenStateHolderCreator = factory::create.downcast()
}