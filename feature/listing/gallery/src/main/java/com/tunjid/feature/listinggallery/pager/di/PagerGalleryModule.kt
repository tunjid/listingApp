package com.tunjid.feature.listinggallery.pager.di

import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.listinggallery.pager.FullscreenGalleryScreen
import com.tunjid.feature.listinggallery.pager.PagerGalleryStateHolderFactory
import com.tunjid.feature.listinggallery.pager.PagerGalleryViewModel
import com.tunjid.feature.listinggallery.pager.State
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.UiState
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

private const val RoutePattern = "/listings/{listingId}/gallery/pager"

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
            routeMapper = ::routeOf
        )

    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    fun routeAdaptiveConfiguration(
        factory: PagerGalleryStateHolderFactory
    ) = threePaneListDetailStrategy { route ->
        val lifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycle.coroutineScope
        val viewModel = viewModel<PagerGalleryViewModel> {
            factory.create(
                scope = lifecycleCoroutineScope,
                route = route,
            )
        }
//        ScreenUiState(
//            UiState(
//                fabShows = false,
//                navVisibility = NavVisibility.Gone,
//                insetFlags = InsetFlags.NONE
//            )
//        )
        FullscreenGalleryScreen(
            movableSharedElementScope = requireThreePaneMovableSharedElementScope(),
            modifier = Modifier,
            state = viewModel.state.collectAsStateWithLifecycle().value,
            actions = viewModel.accept
        )
    }
}