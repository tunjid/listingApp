package com.tunjid.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.canAnimateOnStartingFrames
import com.tunjid.scaffold.treenav.adaptive.threepane.configurations.movableSharedElementConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import com.tunjid.treenav.adaptive.AdaptivePaneState
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route

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
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
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
                            positionalState = globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
                                mapper = UiState::uiChromeState
                            ).value,
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
                    globalUiStateHolder = globalUiStateHolder,
                )
                AppBottomNav(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppSnackBar(
                    globalUiStateHolder = globalUiStateHolder,
                )
            }
        }
    }

    LaunchedEffect(listingAppState) {
        listingAppState.start()
    }
}
