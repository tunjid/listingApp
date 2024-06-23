package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tunjid.treenav.strings.Route

interface ScreenStateHolderCache {
    fun <T : ViewModel> screenStateHolderFor(route: Route, lazyCreate: Boolean = true): T?
}

val LocalViewModelFactory: ProvidableCompositionLocal<ViewModelProvider.Factory> =
    staticCompositionLocalOf {
        TODO("Not yet implemented")
    }

@Composable
inline fun <reified T : ViewModel> viewModel(): T =
    viewModel<T>(factory = LocalViewModelFactory.current)
