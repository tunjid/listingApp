package com.tunjid.listing

import android.app.Application
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.tunjid.airbnb.BuildConfig
import com.tunjid.listing.workmanager.initializers.Sync
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.di.AdaptiveRouter
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
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteTrie
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
    val adaptiveRouter: AdaptiveRouter
    val adaptiveContentStateCreator: (CoroutineScope, SaveableStateHolder) -> AdaptiveContentState
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
    override val adaptiveRouter: AdaptiveRouter,
    override val navigationStateHolder: NavigationStateHolder,
    override val globalUiStateHolder: GlobalUiStateHolder,
    override val lifecycleStateHolder: LifecycleStateHolder,
    override val adaptiveContentStateCreator: (@JvmSuppressWildcards CoroutineScope, @JvmSuppressWildcards SaveableStateHolder) -> @JvmSuppressWildcards AdaptiveContentState,
    private val savedStateCache: SavedStateCache,
    private val allScreenStateHolders: Map<String, @JvmSuppressWildcards ScreenStateHolderCreator>,
) : ListingApp {
    private val routeStateHolderCache = mutableMapOf<Route, ScopeHolder?>()

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
        private val stateHolderTrie = RouteTrie<ScreenStateHolderCreator>().apply {
            allScreenStateHolders
                .mapKeys { (template) -> PathPattern(template) }
                .forEach(::set)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> screenStateHolderFor(route: Route): T? =
            routeStateHolderCache.getOrPut(route) {
                val stateHolderCreator = stateHolderTrie[route] ?: return@getOrPut null

                val routeScope = CoroutineScope(
                    SupervisorJob() + Dispatchers.Main.immediate
                )
                ScopeHolder(
                    scope = routeScope,
                    stateHolder = stateHolderCreator(
                        routeScope,
                        savedStateCache(route),
                        route
                    )
                )

            }?.stateHolder as? T
    }

    private fun MultiStackNav.toSavedState(
        byteSerializer: ByteSerializer,
    ) = SavedState(
        isEmpty = false,
        activeNav = currentIndex,
        navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.children
                    .filterIsInstance<Route>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.id
                    }
            )
        },
        routeStates = flatten(order = Order.BreadthFirst)
            .filterIsInstance<Route>()
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