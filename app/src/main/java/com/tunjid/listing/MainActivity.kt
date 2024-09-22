package com.tunjid.listing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.listing.ui.theme.ListingAppTheme
import com.tunjid.mutator.mutationOf
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.NavMode
import com.tunjid.scaffold.globalui.insetMutations
import com.tunjid.scaffold.globalui.integrateBackActions
import com.tunjid.scaffold.lifecycle.LocalLifecycleStateHolder
import com.tunjid.scaffold.scaffold.Scaffold
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
private fun AdaptNavigation(globalUiStateHolder: GlobalUiStateHolder) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    LaunchedEffect(windowSizeClass) {
        globalUiStateHolder.accept(mutationOf {
            copy(
                windowSizeClass = windowSizeClass,
                navMode = when (windowSizeClass.minWidthDp) {
                    in 0..<WindowSizeClass.COMPACT.minWidthDp -> NavMode.BottomNav
                    else -> NavMode.NavRail
                }
            )
        })
    }
}