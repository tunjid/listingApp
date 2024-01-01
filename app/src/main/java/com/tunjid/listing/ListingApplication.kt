package com.tunjid.listing

import android.app.Application
import com.tunjid.airbnb.BuildConfig
import com.tunjid.listing.workmanager.initializers.Sync
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.adaptive.AdaptiveRoute
import com.tunjid.scaffold.adaptive.StatelessRoute
import com.tunjid.scaffold.di.SavedStateCache
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.scaffold.lifecycle.ScreenStateHolderCache
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.navigation.removedRoutes
import com.tunjid.scaffold.savedstate.SavedState
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.scaffold.toBytes
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import com.tunjid.treenav.strings.RouteParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class ListingApplication : Application() {
    @Inject
    lateinit var listingApp: ListingApp

    override fun onCreate() {
        super.onCreate()
        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
    }
}

interface ListingApp {
    val routeParser: RouteParser<AdaptiveRoute>
    val navigationStateHolder: NavigationStateHolder
    val globalUiStateHolder: GlobalUiStateHolder
    val lifecycleStateHolder: LifecycleStateHolder
    val screenStateHolderCache: ScreenStateHolderCache
}

@Singleton
class PersistedListingApp @Inject constructor(
    appScope: CoroutineScope,
    byteSerializer: ByteSerializer,
    navigationStateStream: StateFlow<MultiStackNav>,
    savedStateRepository: SavedStateRepository,
    override val routeParser: RouteParser<@JvmSuppressWildcards AdaptiveRoute>,
    override val navigationStateHolder: NavigationStateHolder,
    override val globalUiStateHolder: GlobalUiStateHolder,
    override val lifecycleStateHolder: LifecycleStateHolder,
    private val savedStateCache: SavedStateCache,
    private val allScreenStateHolders: Map<Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>,
) : ListingApp {
    private val routeStateHolderCache = mutableMapOf<AdaptiveRoute, ScopeHolder>()

    init {
        navigationStateStream
            .removedRoutes()
            .onEach { removedRoutes ->
                removedRoutes.forEach { route ->
                    if (BuildConfig.DEBUG) println("Cleared ${route::class.simpleName}")
                    val holder = routeStateHolderCache.remove(route)
                    holder?.scope?.cancel()
                }
            }
            .launchIn(appScope)

        lifecycleStateHolder.state
            .map { it.isInForeground }
            .distinctUntilChanged()
            .onStart { emit(false) }
            .flatMapLatest {
                navigationStateStream
                    .mapLatest { navState ->
                        navState.toSavedState(byteSerializer)
                    }
            }
            .onEach(savedStateRepository::saveState)
            .launchIn(appScope)
    }

    override val screenStateHolderCache: ScreenStateHolderCache = object : ScreenStateHolderCache {
        @Suppress("UNCHECKED_CAST")
        override fun <T> screenStateHolderFor(route: AdaptiveRoute): T =
            routeStateHolderCache.getOrPut(route) {
                val routeScope = CoroutineScope(
                    SupervisorJob() + Dispatchers.Main.immediate
                )
                ScopeHolder(
                    scope = routeScope,
                    stateHolder = when (route) {
                        is StatelessRoute -> route
                        else -> allScreenStateHolders
                            .getValue(route::class.java)
                            .invoke(routeScope, savedStateCache(route), route)
                    }
                )

            }.stateHolder as T
    }

    private fun MultiStackNav.toSavedState(
        byteSerializer: ByteSerializer,
    ) = SavedState(
        isEmpty = false,
        activeNav = currentIndex,
        navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.children
                    .filterIsInstance<AdaptiveRoute>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.id
                    }
            )
        },
        routeStates = flatten(order = Order.BreadthFirst)
            .filterIsInstance<AdaptiveRoute>()
            .fold(mutableMapOf()) { map, route ->
                val stateHolder = screenStateHolderCache.screenStateHolderFor<Any>(route)
                val state = (stateHolder as? ActionStateProducer<*, *>)?.state ?: return@fold map
                val serializable = (state as? StateFlow<*>)?.value ?: return@fold map
                if (serializable is ByteSerializable) map[route.id] =
                    byteSerializer.toBytes(serializable)
                map
            })
}

private data class ScopeHolder(
    val scope: CoroutineScope,
    val stateHolder: Any,
)

@Module
@InstallIn(SingletonComponent::class)
interface DirectoryAppModule {
    @Binds
    fun bindDirectoryApp(app: PersistedListingApp): ListingApp
}