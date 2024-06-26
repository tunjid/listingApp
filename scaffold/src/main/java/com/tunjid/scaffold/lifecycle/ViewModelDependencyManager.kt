package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.navigation.removedRoutes
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

interface ViewModelDependencyManager {
    /**
     * Creates a [ViewModelStoreOwner] for a given [Route]
     */
    fun viewModelStoreOwnerFor(route: Route): ViewModelStoreOwner

    /**
     * Creates a [ViewModelProvider.Factory] capable of injecting the route it was created for
     * into the [ViewModel]
     */
    fun viewModelFactoryFor(route: Route): ViewModelProvider.Factory
}

val LocalViewModelFactory: ProvidableCompositionLocal<ViewModelProvider.Factory> =
    staticCompositionLocalOf {
        TODO("Not yet implemented")
    }

@Composable
inline fun <reified T : ViewModel> viewModel(): T =
    viewModel<T>(factory = LocalViewModelFactory.current)

/**
 * Manages ViewModel dependencies app-wide
 */
class AppViewModelDependencyManager @Inject constructor(
    appScope: CoroutineScope,
    navigationStateStream: StateFlow<MultiStackNav>,
    private val allScreenStateHolders: Map<@JvmSuppressWildcards Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>,

    ) : ViewModelDependencyManager {

    private val routeToViewModelStoreOwner = mutableMapOf<String, ViewModelStoreOwner>()

    init {
        navigationStateStream
            .removedRoutes()
            .onEach { removedRoutes ->
                removedRoutes.forEach { route ->
                    val owner = routeToViewModelStoreOwner.remove(route.id)
                    owner?.viewModelStore?.clear()
                }
            }
            .launchIn(appScope)
    }

    override fun viewModelStoreOwnerFor(
        route: Route
    ): ViewModelStoreOwner = routeToViewModelStoreOwner.getOrPut(
        route.id
    ) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    }


    override fun viewModelFactoryFor(
        route: Route
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T = allScreenStateHolders.getValue(modelClass).create(
            scope = CoroutineScope(
                context = SupervisorJob() + Dispatchers.Main.immediate
            ),
            route = route
        ) as T
    }
}


