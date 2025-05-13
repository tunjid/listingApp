package com.tunjid.listing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.tunjid.listing.ui.theme.ListingAppTheme
import com.tunjid.scaffold.globalui.PredictiveBackEffects
import com.tunjid.scaffold.scaffold.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = applicationContext as ListingApplication
        val listingApp = app.listingApp
        val appState = listingApp.appState

        setContent {
            ListingAppTheme {
                App(
                    modifier = Modifier,
                    appState = appState,
                )
                PredictiveBackEffects(
                    appState = appState,
                )
            }
            App(
                modifier = Modifier,
                appState = appState,
            )
            PredictiveBackEffects(
                appState = appState,
            )
        }
    }
}
