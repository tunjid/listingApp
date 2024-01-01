package com.tunjid.scaffold.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.tunjid.scaffold.adaptive.AdaptiveRoute

interface ScreenStateHolderCache {
    fun <T> screenStateHolderFor(route: AdaptiveRoute): T
}

val LocalScreenStateHolderCache: ProvidableCompositionLocal<ScreenStateHolderCache> =
    staticCompositionLocalOf {
        object : ScreenStateHolderCache {
            override fun <T> screenStateHolderFor(route: AdaptiveRoute): T {
                TODO("Not yet implemented")
            }
        }
    }

@Composable
fun <T> rememberRetainedStateHolder(route: AdaptiveRoute): T {
    val cache = LocalScreenStateHolderCache.current
    return remember(cache) {
        cache.screenStateHolderFor(route)
    }
}