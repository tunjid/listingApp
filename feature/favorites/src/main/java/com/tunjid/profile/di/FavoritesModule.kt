package com.tunjid.profile.di

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.adaptive.StatelessRoute
import com.tunjid.scaffold.di.UrlRouteMatcherBinding
import com.tunjid.scaffold.navigation.SerializedRouteParams
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.urlRouteMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import kotlinx.serialization.Serializable

private const val RoutePattern = "favorites"

@Serializable
data class FavoritesRoute(
    override val routeParams: SerializedRouteParams
) : AdaptiveRoute, StatelessRoute {

    @Composable
    override fun Content() {
       FavoritesRoute(route = this)
    }
}

@Composable
private fun FavoritesRoute(route: FavoritesRoute) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = route.id
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {
    @IntoMap
    @Provides
    @StringKey(RoutePattern)
    @UrlRouteMatcherBinding
    fun routeParser(): UrlRouteMatcher<@JvmSuppressWildcards AdaptiveRoute> =
        urlRouteMatcher(
            routePattern = RoutePattern,
            routeMapper = ::FavoritesRoute
        )

}