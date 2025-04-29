package com.tunjid.listing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.listing.ui.theme.ListingAppTheme
import com.tunjid.scaffold.globalui.MEDIUM
import com.tunjid.scaffold.globalui.NavMode
import com.tunjid.scaffold.globalui.PredictiveBackEffects
import com.tunjid.scaffold.scaffold.AppState
import com.tunjid.scaffold.scaffold.ListingApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = applicationContext as ListingApplication
        val listingApp = app.listingApp
        val appState = listingApp.appState

        setContent {
            ListingAppTheme {
                ListingApp(
                    modifier = Modifier,
                    appState = appState,
                )
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
    }
}