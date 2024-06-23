package com.tunjid.listing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.tunjid.listing.ui.theme.ListingAppTheme
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.NavMode
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.integrateBackActions
import com.tunjid.scaffold.globalui.toWindowSizeClass
import com.tunjid.scaffold.lifecycle.LocalLifecycleStateHolder
import com.tunjid.scaffold.scaffold.Scaffold
import com.tunjid.mutator.mutationOf
import com.tunjid.scaffold.globalui.insetMutations
import com.tunjid.scaffold.lifecycle.LocalViewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as ListingApplication
        val listingApp = app.listingApp

        integrateBackActions(
            globalUiStateHolder = listingApp.globalUiStateHolder,
            navStateHolder = listingApp.navigationStateHolder,
        )

        setContent {
            ListingAppTheme {
                CompositionLocalProvider(
                    LocalViewModelFactory provides listingApp.screenStateHolderCache,
                    LocalLifecycleStateHolder provides listingApp.lifecycleStateHolder,
                ) {
                    Scaffold(
                        modifier = Modifier,
                        adaptiveContentState = listingApp.adaptiveContentState(),
                        navStateHolder = listingApp.navigationStateHolder,
                        globalUiStateHolder = listingApp.globalUiStateHolder,
                    )
                }
                AdaptNavigation(globalUiStateHolder = listingApp.globalUiStateHolder)
            }
        }

        lifecycleScope.launch {
            insetMutations().collect(listingApp.globalUiStateHolder.accept)
        }
        lifecycleScope.launch {
            listingApp.globalUiStateHolder.state
                .map { it.statusBarColor to it.navBarColor }
                .distinctUntilChanged()
                .collect { (statusBarColor, navBarColor) ->
                    window.statusBarColor = statusBarColor
                    window.navigationBarColor = navBarColor
                }
        }
    }
}

@Composable
private fun MainActivity.AdaptNavigation(globalUiStateHolder: GlobalUiStateHolder) {
    val configuration = LocalConfiguration.current
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
    }
    val windowDpSize = with(LocalDensity.current) {
        windowMetrics.bounds.toComposeRect().size.toDpSize()
    }
    val widthWindowSizeClass = windowDpSize.width.toWindowSizeClass()

    LaunchedEffect(widthWindowSizeClass) {
        globalUiStateHolder.accept(mutationOf {
            copy(
                windowSizeClass = widthWindowSizeClass,
                navMode = when (widthWindowSizeClass) {
                    WindowSizeClass.COMPACT -> NavMode.BottomNav
                    WindowSizeClass.MEDIUM -> NavMode.NavRail
                    WindowSizeClass.EXPANDED -> NavMode.NavRail
                }
            )
        })
    }
}