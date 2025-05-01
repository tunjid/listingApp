package com.tunjid.scaffold.di

import android.content.Context
import androidx.lifecycle.ViewModel
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.DelegatingByteSerializer
import com.tunjid.scaffold.fromBytes
import com.tunjid.scaffold.media.ExoPlayerManager
import com.tunjid.scaffold.media.PlayerManager
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.scaffold.savedstate.DataStoreSavedStateRepository
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
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
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Singleton

interface ScreenStateHolderCreator {
    fun create(
        scope: CoroutineScope,
        route: Route,
    ): ViewModel
}

data class SavedStateType(
    val apply: PolymorphicModuleBuilder<ByteSerializable>.() -> Unit
)

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
    fun appScope(): CoroutineScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.Main.immediate
    )

    @Provides
    @Singleton
    fun savedStatePath(
        @ApplicationContext context: Context
    ): Path = context.filesDir.resolve("savedState").absolutePath.toPath()

    @Provides
    @Singleton
    fun byteSerializer(): ByteSerializer = DelegatingByteSerializer(
        format = ProtoBuf {
            serializersModule = SerializersModule {

            }
        }
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
    fun defaultSavedState(): Set<@JvmSuppressWildcards SavedStateType>

    @Multibinds
    fun defaultStateHolderCreators(): Map<Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>

    @Binds
    fun bindNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder
    ): NavigationStateHolder

    @Binds
    fun bindSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateRepository
    ): SavedStateRepository

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