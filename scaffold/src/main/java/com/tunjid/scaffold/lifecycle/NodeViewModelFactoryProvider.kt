package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.treenav.Node
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@Stable
interface NodeViewModelFactoryProvider {
    /**
     * Creates a [ViewModelProvider.Factory] capable of injecting the route it was created for
     * into the [ViewModel]
     */
    fun viewModelFactoryFor(node: Node): ViewModelProvider.Factory
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
class AppNodeViewModelFactoryProvider @Inject constructor(
    private val allScreenStateHolders: Map<@JvmSuppressWildcards Class<*>, @JvmSuppressWildcards ScreenStateHolderCreator>,
) : NodeViewModelFactoryProvider {

    override fun viewModelFactoryFor(
        node: Node
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T = allScreenStateHolders.getValue(modelClass).create(
            scope = CoroutineScope(
                context = SupervisorJob() + Dispatchers.Main.immediate
            ),
            route = node as Route
        ) as T
    }
}


