package com.tunjid.scaffold.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.tunjid.mutator.Mutation
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.DelegatingByteSerializer
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.adaptive.AdaptiveRouteConfiguration
import com.tunjid.scaffold.fromBytes
import com.tunjid.scaffold.globalui.ActualGlobalUiStateHolder
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.lifecycle.ActualLifecycleStateHolder
import com.tunjid.scaffold.lifecycle.Lifecycle
import com.tunjid.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.scaffold.navigation.RouteNotFound
import com.tunjid.scaffold.savedstate.DataStoreSavedStateRepository
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.scaffold.scaffold.AdaptiveContentStateFactory
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.RouteTrie
import com.tunjid.treenav.strings.UrlRouteMatcher
import com.tunjid.treenav.strings.routeParserFrom
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Singleton

typealias ScreenStateHolderCreator = (CoroutineScope, ByteArray?, Route) -> Any

typealias SavedStateCache = (@JvmSuppressWildcards Route) -> ByteArray?

data class SavedStateType(
    val apply: PolymorphicModuleBuilder<ByteSerializable>.() -> Unit
)

interface AdaptiveRouter {
    fun destination(route: Route): @Composable () -> Unit

    fun secondaryRouteFor(route: Route): Route?

    fun transitionsFor(state: Adaptive.ContainerState): Adaptive.Transitions?
}

inline fun <reified T : ByteSerializable> ByteSerializer.restoreState(savedState: ByteArray?): T? {
    return try {
        // Polymorphic serialization requires that the compile time type used to serialize, must also be used to
        // deserialize. See https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
        if (savedState != null) fromBytes<ByteSerializable>(savedState) as? T else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ScaffoldModule {
    @Provides
    @Singleton
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    fun savedStatePath(
        @ApplicationContext context: Context
    ): Path = context.filesDir.resolve("savedState").absolutePath.toPath()

    @Provides
    @Singleton
    fun byteSerializer(
        savedStateTypes: Set<@JvmSuppressWildcards SavedStateType>
    ): ByteSerializer = DelegatingByteSerializer(
        format = ProtoBuf {
            serializersModule = SerializersModule {
                polymorphic(ByteSerializable::class) {
                    savedStateTypes.forEach { it.apply(this) }
                }
            }
        }
    )

    @Provides
    @Singleton
    fun routeParser(
        routeMatcherMap: Map<String, @JvmSuppressWildcards UrlRouteMatcher<Route>>
    ): RouteParser<@JvmSuppressWildcards Route> {
        val routeMatchers = routeMatcherMap
            .toList()
            .sortedWith(routeMatchingComparator())
            .map(Pair<String, @kotlin.jvm.JvmSuppressWildcards UrlRouteMatcher<Route>>::second)
        return routeParserFrom(*(routeMatchers).toTypedArray())
    }

    @Provides
    @Singleton
    fun router(
        routeConfigurationMap: Map<String, @JvmSuppressWildcards AdaptiveRouteConfiguration>,
    ): AdaptiveRouter {
        val configurationTrie = RouteTrie<AdaptiveRouteConfiguration>().apply {
            routeConfigurationMap
                .mapKeys { (template) -> PathPattern(template) }
                .forEach(::set)
        }

        return object : AdaptiveRouter {
            override fun secondaryRouteFor(route: Route): Route? =
                configurationTrie[route]?.secondaryRoute(route)

            override fun transitionsFor(state: Adaptive.ContainerState): Adaptive.Transitions? =
                state.currentRoute?.let(configurationTrie::get)?.transitionsFor(state)


            override fun destination(route: Route): @Composable () -> Unit = {
                configurationTrie[route]?.Render(route) ?: RouteNotFound()
            }
        }
    }

    @Provides
    @Singleton
    fun savedStateCache(
        savedStateRepository: SavedStateRepository
    ): SavedStateCache = { route ->
        savedStateRepository.savedState.value.routeStates[route.id]
    }

    @Provides
    fun navStateStream(
        navigationStateHolder: NavigationStateHolder
    ): StateFlow<MultiStackNav> = navigationStateHolder.state

    @Provides
    fun navActions(
        navigationStateHolder: NavigationStateHolder
    ): (@JvmSuppressWildcards NavigationMutation) -> Unit = navigationStateHolder.accept

    @Provides
    fun globalUiStateStream(
        globalUiStateHolder: GlobalUiStateHolder
    ): StateFlow<UiState> = globalUiStateHolder.state

    @Provides
    fun globalUiActions(
        globalUiStateHolder: GlobalUiStateHolder
    ): (Mutation<UiState>) -> Unit = globalUiStateHolder.accept

    @Provides
    fun lifecycleStateStream(
        lifecycleStateHolder: LifecycleStateHolder
    ): StateFlow<Lifecycle> = lifecycleStateHolder.state

    @Provides
    fun adaptiveContentStateCreator(
        factory: AdaptiveContentStateFactory
    ): (@JvmSuppressWildcards CoroutineScope, @JvmSuppressWildcards SaveableStateHolder) -> @JvmSuppressWildcards AdaptiveContentState = factory::create
}

@Module
@InstallIn(SingletonComponent::class)
interface ScaffoldBindModule {

    @Multibinds
    fun defaultRouteMatchers(): Map<String, @JvmSuppressWildcards UrlRouteMatcher<Route>>

    @Multibinds
    fun defaultSavedState(): Set<@JvmSuppressWildcards SavedStateType>

    @Multibinds
    fun defaultStateHolderCreators(): Map<Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>

    @Binds
    fun bindNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder
    ): NavigationStateHolder

    @Binds
    fun bindGlobalUiStateHolder(
        actualGlobalUiStateHolder: ActualGlobalUiStateHolder
    ): GlobalUiStateHolder

    @Binds
    fun bindLifecycleStateHolder(
        actualLifecycleStateHolder: ActualLifecycleStateHolder
    ): LifecycleStateHolder

    @Binds
    fun bindSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateRepository
    ): SavedStateRepository
}

private fun routeMatchingComparator() =
    compareBy<Pair<String, UrlRouteMatcher<Route>>>(
        // Order by number of path segments firs
        { (key) -> key.split("/").size },
        // Match more specific segments first, route params should be matched later
        { (key) -> -key.split("/").filter { it.startsWith("{") }.size },
        // Finally sort alphabetically
        { (key) -> key }
    ).reversed()