package com.tunjid.scaffold.globalui

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import com.tunjid.mutator.Mutation
import com.tunjid.scaffold.globalui.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Drives global UI that is common from screen to screen described by a [UiState].
 * This makes it so that these persistent UI elements aren't duplicated, and only animate themselves when they change.
 * This is the default implementation of [GlobalUiController] that other implementations of
 * the same interface should delegate to.
 */

fun ComponentActivity.insetMutations(): Flow<Mutation<UiState>> {
    enableEdgeToEdge()

    val rootView = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0)

    return callbackFlow {
        rootView.setOnApplyWindowInsetsListener { _, insets ->
            channel.trySend {
                reduceSystemInsets(
                    WindowInsetsCompat.toWindowInsetsCompat(insets),
                    0
                )
                // Consume insets so other views will not see them.
            }
            insets
        }
        awaitClose { }
    }
}

private fun UiState.reduceSystemInsets(
    windowInsets: WindowInsetsCompat,
    navBarHeightThreshold: Int
): UiState {
    // Do this once, first call is the size
    val currentSystemUI = systemUI
    val currentStaticSystemUI = currentSystemUI.static

    val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
    val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
    val cutouts = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
    val captions = windowInsets.getInsets(WindowInsetsCompat.Type.captionBar())
    val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

    val updatedStaticUI = when {
        currentStaticSystemUI !is DelegateStaticSystemUI -> DelegateStaticSystemUI(
            statusBarSize = statusBars.top,
            navBarSize = navBars.bottom
        )

        navBars.bottom < navBarHeightThreshold -> DelegateStaticSystemUI(
            statusBarSize = statusBars.top,
            navBarSize = navBars.bottom
        )

        else -> currentStaticSystemUI.copy(
            statusBarSize = statusBars.top,
        )
    }

    val updatedDynamicUI = DelegateDynamicSystemUI(
        statusBars = Ingress(top = statusBars.top, bottom = statusBars.bottom),
        navBars = Ingress(top = navBars.top, bottom = navBars.bottom),
        cutouts = Ingress(top = cutouts.top, bottom = cutouts.bottom),
        captions = Ingress(top = captions.top, bottom = captions.bottom),
        ime = Ingress(top = ime.top, bottom = ime.bottom),
        snackbarHeight = currentSystemUI.dynamic.snackbarHeight
    )

    return copy(
        systemUI = DelegateSystemUI(
            static = updatedStaticUI,
            dynamic = updatedDynamicUI
        )
    )
}