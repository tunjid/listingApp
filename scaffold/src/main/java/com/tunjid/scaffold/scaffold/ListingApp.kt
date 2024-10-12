package com.tunjid.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.tunjid.scaffold.scaffold.configuration.predictiveBackConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.threepane.configurations.threePanedMovableSharedElementConfiguration
import com.tunjid.treenav.compose.threepane.configurations.threePanedNavHostConfiguration
import com.tunjid.treenav.strings.Route

/**
 * Root scaffold for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ListingApp(
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
                            canAnimateOnStartingFrames = PaneState<ThreePane, Route>::canAnimateOnStartingFrames
                        )
                    }
                    PanedNavHost(
                        state = listingAppState.rememberPanedNavHostState {
                            val windowSizeClassState = derivedStateOf {
                                listingAppState.globalUi.windowSizeClass
                            }
                            val backStatusState = derivedStateOf {
                                listingAppState.globalUi.backStatus
                            }
                            this
                                .threePanedNavHostConfiguration(
                                    windowWidthDpState = derivedStateOf { 
                                        windowSizeClassState.value.minWidthDp
                                    }
                                )
                                .predictiveBackConfiguration(
                                    windowSizeClassState = windowSizeClassState,
                                    backStatusState = backStatusState,
                                )
                                .threePanedMovableSharedElementConfiguration(
                                    movableSharedElementHostState
                                )
                        },
                        modifier = Modifier.fillMaxSize()
                                then movableSharedElementHostState.modifier
                                then sharedElementModifier
                    ) {
                        ThreePaneLayout(
                            uiChromeState = remember {
                                derivedStateOf { listingAppState.globalUi.uiChromeState }
                            }.value,
                            onPaneAnchorChanged = remember {
                                { paneAnchor: PaneAnchor ->
                                    listingAppState.updateGlobalUi {
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
}
