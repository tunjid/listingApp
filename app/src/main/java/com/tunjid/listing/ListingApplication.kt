package com.tunjid.listing

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.tunjid.listing.workmanager.initializers.Sync
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.savedstate.SavedState
import com.tunjid.scaffold.savedstate.SavedStateRepository
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
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
class ListingApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var listingApp: ListingApp

    override fun onCreate() {
        super.onCreate()
        ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()

        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()
}

interface ListingApp {
    val adaptiveContentState: AdaptiveContentState
    val navigationStateHolder: NavigationStateHolder
    val globalUiStateHolder: GlobalUiStateHolder
    val lifecycleStateHolder: LifecycleStateHolder
}

@Singleton
class PersistedListingApp @Inject constructor(
    appScope: CoroutineScope,
    navigationStateStream: StateFlow<MultiStackNav>,
    savedStateRepository: SavedStateRepository,
    override val navigationStateHolder: NavigationStateHolder,
    override val globalUiStateHolder: GlobalUiStateHolder,
    override val lifecycleStateHolder: LifecycleStateHolder,
    override val adaptiveContentState: AdaptiveContentState,
) : ListingApp {

    init {
        lifecycleStateHolder.state
            .map { it.isInForeground }
            .distinctUntilChanged()
            .onStart { emit(false) }
            .flatMapLatest {
                navigationStateStream
                    .mapLatest { navState ->
                        navState.toSavedState()
                    }
            }
            .onEach(savedStateRepository::saveState)
            .launchIn(appScope)
    }

    private fun MultiStackNav.toSavedState() = SavedState(
        isEmpty = false,
        activeNav = currentIndex,
        navigation = stacks.fold(listOf()) { listOfLists, stackNav ->
            listOfLists.plus(
                element = stackNav.children
                    .filterIsInstance<Route>()
                    .fold(listOf()) { stackList, route ->
                        stackList + route.routeParams.pathAndQueries
                    }
            )
        },
        routeStates = emptyMap()
    )
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