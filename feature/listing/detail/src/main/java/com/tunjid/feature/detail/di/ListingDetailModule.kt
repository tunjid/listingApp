package com.tunjid.feature.detail.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.feature.detail.ListingDetailScreen
import com.tunjid.feature.detail.ListingDetailStateHolder
import com.tunjid.feature.detail.ListingStateHolderFactory
import com.tunjid.feature.detail.State
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.adaptive.ExternalRoute
import com.tunjid.scaffold.di.SavedStateType
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.di.UrlRouteMatcherBinding
import com.tunjid.scaffold.di.downcast
import com.tunjid.scaffold.lifecycle.collectAsStateWithLifecycle
import com.tunjid.scaffold.lifecycle.rememberRetainedStateHolder
import com.tunjid.scaffold.navigation.SerializedRouteParams
import com.tunjid.scaffold.scaffold.backPreviewBackgroundModifier
import com.tunjid.treenav.Node
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

private const val ListingDetailRoutePattern = "listings/{listingId}"

@Serializable
data class ListingDetailRoute(
    override val routeParams: SerializedRouteParams
) : AdaptiveRoute {

    val listingId get() = routeParams.pathArgs.getValue("listingId")

    val startingMediaUrls get() = routeParams.queryParams["url"] ?: emptyList()

    val initialQuery = MediaQuery(
        listingId = listingId,
        offset = routeParams.queryParams["pageOffset"]?.first()?.toLongOrNull() ?: 0L,
        limit = routeParams.queryParams["pageLimit"]?.first()?.toLongOrNull() ?: 4L,
    )

    override val children: List<Node> = listOf(
        ExternalRoute(
            path = "listings"
        )
    )

    override val secondaryRoute get() = children.filterIsInstance<ExternalRoute>().first()

    @Composable
    override fun Content() {
        val stateHolder = rememberRetainedStateHolder<ListingDetailStateHolder>(
            route = this@ListingDetailRoute
        )
        ListingDetailScreen(
            modifier = Modifier.backPreviewBackgroundModifier(),
            state = stateHolder.state.collectAsStateWithLifecycle().value,
            actions = stateHolder.accept
        )
    }
}

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
    @StringKey(ListingDetailRoutePattern)
    @UrlRouteMatcherBinding
    fun archiveListRouteParser(): UrlRouteMatcher<@JvmSuppressWildcards AdaptiveRoute> =
        urlRouteMatcher(
            routePattern = ListingDetailRoutePattern,
            routeMapper = ::ListingDetailRoute
        )

    @IntoMap
    @Provides
    @ClassKey(ListingDetailRoute::class)
    fun archiveListStateHolderCreator(
        factory: ListingStateHolderFactory
    ): ScreenStateHolderCreator = factory::create.downcast()
}