package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.Order
import com.tunjid.treenav.flatten
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
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

    fun clearStoreFor(route: Route)
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
@Stable
class AppViewModelDependencyManager @Inject constructor(
    private val navigationStateStream: StateFlow<MultiStackNav>,
    private val allScreenStateHolders: Map<@JvmSuppressWildcards Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>,
) : ViewModelDependencyManager {

    private val routeToViewModelStoreOwner = mutableMapOf<String, ViewModelStoreOwner>()

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

    override fun clearStoreFor(route: Route) {
        if (navigationStateStream.value.flatten(Order.BreadthFirst).contains(route)) {
            return
        }
        println("Clearing VM for $route")
        val owner = routeToViewModelStoreOwner.remove(route.id)
        owner?.viewModelStore?.clear()
    }
}


