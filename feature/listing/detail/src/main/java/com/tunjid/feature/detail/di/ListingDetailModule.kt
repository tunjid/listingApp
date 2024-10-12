package com.tunjid.feature.detail.di

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.feature.detail.ListingDetailScreen
import com.tunjid.feature.detail.ListingDetailViewModel
import com.tunjid.feature.detail.ListingStateHolderFactory
import com.tunjid.feature.detail.State
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.routeOf
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.viewModelCoroutineScope
import com.tunjid.scaffold.scaffold.configuration.predictiveBackBackgroundModifier
import com.tunjid.treenav.compose.threepane.configurations.movableSharedElementScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneListDetailStrategy
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
    fun routeAdaptiveConfiguration(
        factory: ListingStateHolderFactory
    ) = threePaneListDetailStrategy(
        paneMapping = { route ->
            mapOf(
                ThreePane.Primary to route,
                ThreePane.Secondary to route.children.first() as? Route
            )
        },
        render = { route ->
            val viewModel = viewModel<ListingDetailViewModel> {
                factory.create(
                    scope = viewModelCoroutineScope(),
                    route = route,
                )
            }
            ScreenUiState(
                UiState(
                    navVisibility = NavVisibility.Gone,
                    insetFlags = InsetFlags.NONE,
                    statusBarColor = Color.Black.copy(alpha = 0.4f).toArgb()
                )
            )
            ListingDetailScreen(
                movableSharedElementScope = movableSharedElementScope(),
                modifier = Modifier.predictiveBackBackgroundModifier(paneScope = this),
                state = viewModel.state.collectAsStateWithLifecycle().value,
                actions = viewModel.accept
            )
        }
    )
}