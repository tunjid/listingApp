package com.tunjid.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.scaffold.globalui.slices.fabState
import com.tunjid.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.movableSharedElementConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptivePaneState
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Scaffold(
    modifier: Modifier,
    listingAppState: ListingAppState,
    navStateHolder: NavigationStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
        LocalNavigationStateHolder provides navStateHolder,
    ) {
        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    navItems = listingAppState.navItems,
                    uiChromeState = remember {
                        derivedStateOf { listingAppState.globalUi.uiChromeState }
                    }.value,
                    onNavItemSelected = listingAppState::onNavItemSelected,
                )
                // Root LookaheadScope used to anchor all shared element transitions
                SharedTransitionScope { sharedElementModifier ->
                    val movableSharedElementHostState = remember {
                        MovableSharedElementHostState(
                            sharedTransitionScope = this@SharedTransitionScope,
                            canAnimateOnStartingFrames = AdaptivePaneState<ThreePane, Route>::canAnimateOnStartingFrames
                        )
                    }
                    AdaptiveNavHost(
                        state = remember {
                            listingAppState.adaptiveNavHostState {
                                movableSharedElementConfiguration(movableSharedElementHostState)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                                then movableSharedElementHostState.modifier
                                then sharedElementModifier
                    ) {
                        AdaptiveContentScaffold(
                            positionalState = remember {
                                derivedStateOf { listingAppState.globalUi.uiChromeState }
                            }.value,
                            onPaneAnchorChanged = remember {
                                { paneAnchor: PaneAnchor ->
                                    globalUiStateHolder.accept {
                                        copy(paneAnchor = paneAnchor)
                                    }
                                }
                            },
                        )
                    }
                }
                AppFab(
                    state = remember {
                        derivedStateOf { listingAppState.globalUi.fabState }
                    }.value,
                    onClicked = {
                        listingAppState.globalUi.fabClickListener(Unit)
                    }
                )
                AppBottomNav(
                    navItems = listingAppState.navItems,
                    positionalState = remember {
                        derivedStateOf { listingAppState.globalUi.bottomNavPositionalState }
                    }.value,
                    onNavItemSelected = listingAppState::onNavItemSelected,
                )
                AppSnackBar(
                    state = remember {
                        derivedStateOf { listingAppState.globalUi.snackbarPositionalState }
                    }.value,
                    queue = remember {
                        derivedStateOf { listingAppState.globalUi.snackbarMessages }
                    }.value,
                    onMessageClicked = { message ->
                        listingAppState.globalUi.snackbarMessageConsumer(message)
                    },
                    onSnackbarOffsetChanged = { offset ->
                        listingAppState.updateGlobalUi { copy(snackbarOffset = offset) }
                    },
                )
            }
        }
    }

    DisposableEffect(listingAppState) {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            listingAppState.start()
        }
        onDispose { job.cancel() }
    }
}
