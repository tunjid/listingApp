package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.treenav.strings.Route

interface ScreenStateHolderCache {
    fun <T> screenStateHolderFor(route: Route, lazyCreate: Boolean = true): T?
}

val LocalScreenStateHolderCache: ProvidableCompositionLocal<ScreenStateHolderCache> =
    staticCompositionLocalOf {
        object : ScreenStateHolderCache {
            override fun <T> screenStateHolderFor(route: Route, lazyCreate: Boolean): T {
                TODO("Not yet implemented")
            }
        }
    }

@Composable
fun <T> rememberRetainedStateHolder(route: Route): T {
    val cache = LocalScreenStateHolderCache.current
    return remember(cache) {
        cache.screenStateHolderFor(route) as? T ?: throw IllegalArgumentException(
            "No state holder has been registered for route ${route.id}"
        )
    }
}