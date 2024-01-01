package com.tunjid.feature.listinggallery.pager.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.feature.listinggallery.pager.FullscreenGalleryScreen
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolder
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.pager.State
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.di.downcast
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.rememberRetainedStateHolder
import com.tunjid.scaffold.navigation.SerializedRouteParams
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

private const val PagerGalleryPattern = "listings/{listingId}/gallery/pager"

@Serializable
data class PagerGalleryRoute(
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
        val stateHolder = rememberRetainedStateHolder<PagerGalleryStateHolder>(
            route = this@PagerGalleryRoute
        )
        FullscreenGalleryScreen(
            modifier = Modifier,
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }
}

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
    @StringKey(PagerGalleryPattern)
    fun fullscreenGalleryRouteParser(): UrlRouteMatcher<AdaptiveRoute> =
        urlRouteMatcher(
            routePattern = PagerGalleryPattern,
            routeMapper = ::PagerGalleryRoute
        )

    @IntoMap
    @Provides
    @ClassKey(PagerGalleryRoute::class)
    fun fullscreenGalleryStateHolderCreator(
        factory: PagerGalleryStateHolderFactory
    ): ScreenStateHolderCreator = factory::create.downcast()
}