package com.tunjid.listing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.listing.ui.theme.ListingAppTheme
import com.tunjid.scaffold.globalui.MEDIUM
import com.tunjid.scaffold.globalui.NavMode
import com.tunjid.scaffold.globalui.PredictiveBackEffects
import com.tunjid.scaffold.globalui.insetMutations
import com.tunjid.scaffold.savedstate.SavedState
import com.tunjid.scaffold.scaffold.ListingApp
import com.tunjid.scaffold.scaffold.ListingAppState
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as ListingApplication
        val listingApp = app.listingApp
        val appState = listingApp.appState

        setContent {
            ListingAppTheme {
                ListingApp(
                    modifier = Modifier,
                    listingAppState = appState,
                )
                AdaptNavigation(appState = appState)
                PredictiveBackEffects(
                    appState = appState,
                )
                LaunchedEffect(appState.globalUi.statusBarColor) {
                    window.statusBarColor = appState.globalUi.statusBarColor
                }
                LaunchedEffect(appState.globalUi.navBarColor) {
                    window.navigationBarColor = appState.globalUi.navBarColor
                }
            }
        }
        lifecycleScope.launch {
            insetMutations().collect(appState::updateGlobalUi)
        }
    }
}

@Composable
private fun AdaptNavigation(appState: ListingAppState) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    LaunchedEffect(windowSizeClass) {
        appState.updateGlobalUi {
            copy(
                windowSizeClass = windowSizeClass,
                navMode = when (windowSizeClass.minWidthDp) {
                    in WindowSizeClass.MEDIUM.minWidthDp..Int.MAX_VALUE -> NavMode.NavRail
                    else -> NavMode.BottomNav
                }
            )
        }
    }
}