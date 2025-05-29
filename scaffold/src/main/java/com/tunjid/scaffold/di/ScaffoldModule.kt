package com.tunjid.scaffold.di

import androidx.lifecycle.ViewModel
import com.tunjid.scaffold.media.ExoPlayerManager
import com.tunjid.scaffold.media.PlayerManager
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

interface AssistedViewModelFactory {
    fun create(
        scope: CoroutineScope,
        route: Route,
    ): ViewModel
}

@Module
@InstallIn(SingletonComponent::class)
object ScaffoldModule {
    @Provides
    @Singleton
    fun appScope(): CoroutineScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.Main.immediate
    )

    @Provides
    @Singleton
    fun routeParser(
        routeMatcherMap: Map<String, @JvmSuppressWildcards RouteMatcher>
    ): RouteParser {
        val routeMatchers = routeMatcherMap
            .toList()
            .sortedWith(routeMatchingComparator())
            .map(Pair<String, @kotlin.jvm.JvmSuppressWildcards RouteMatcher>::second)
        return routeParserFrom(*(routeMatchers).toTypedArray())
    }

    @Provides
    fun navStateStream(
        navigationStateHolder: NavigationStateHolder
    ): StateFlow<MultiStackNav> = navigationStateHolder.state

    @Provides
    fun navActions(
        navigationStateHolder: NavigationStateHolder
    ): (@JvmSuppressWildcards NavigationMutation) -> Unit = navigationStateHolder.accept

}

@Module
@InstallIn(SingletonComponent::class)
interface ScaffoldBindModule {

    @Multibinds
    fun defaultRouteMatchers(): Map<String, @JvmSuppressWildcards RouteMatcher>

    @Multibinds
    fun defaultStateHolderCreators(): Map<Class<*>, @JvmSuppressWildcards AssistedViewModelFactory>

    @Binds
    fun bindNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder
    ): NavigationStateHolder

    @Binds
    fun bindPlayerManager(
        playerManager: ExoPlayerManager
    ): PlayerManager
}

private fun routeMatchingComparator() =
    compareBy<Pair<String, RouteMatcher>>(
        // Order by number of path segments firs
        { (key) -> key.split("/").size },
        // Match more specific segments first, route params should be matched later
        { (key) -> -key.split("/").filter { it.startsWith("{") }.size },
        // Finally sort alphabetically
        { (key) -> key }
    ).reversed()