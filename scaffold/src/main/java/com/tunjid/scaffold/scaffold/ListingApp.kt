package com.tunjid.scaffold.scaffold

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.scaffold.globalui.slices.fabState
import com.tunjid.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.scaffold.scaffold.configuration.predictiveBackConfiguration
import com.tunjid.treenav.compose.PaneState
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.configurations.animatePaneBoundsConfiguration
import com.tunjid.treenav.compose.configurations.paneModifierConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.configurations.canAnimateOnStartingFrames
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
    val paneRenderOrder = remember {
        listOf(
            ThreePane.Secondary,
            ThreePane.Primary,
        )
    }
    val splitLayoutState = remember {
        SplitLayoutState(
            orientation = Orientation.Horizontal,
            maxCount = paneRenderOrder.size,
            minSize = Dp.Hairline,
            keyAtIndex = { index ->
                val indexDiff = paneRenderOrder.size - visibleCount
                paneRenderOrder[index + indexDiff]
            }
        )
    }
    val density = LocalDensity.current
    val paneAnchorState = remember { PaneAnchorState(density) }
    val dragToDismissState = remember { DragToDismissState() }

    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
        LocalNavigationStateHolder provides navStateHolder,
        LocalPaneAnchorState provides paneAnchorState,
        LocalDragToDismissState provides dragToDismissState,
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
                        modifier = Modifier.fillMaxSize(),
                        state = listingAppState.rememberPanedNavHostState {
                            this
                                .threePanedNavHostConfiguration(
                                    windowWidthState = derivedStateOf {
                                        splitLayoutState.size
                                    }
                                )
                                .predictiveBackConfiguration(
                                    windowSizeClassState = derivedStateOf {
                                        listingAppState.globalUi.windowSizeClass
                                    },
                                    backStatusState = derivedStateOf {
                                        listingAppState.globalUi.backStatus
                                    },
                                )
                                .threePanedMovableSharedElementConfiguration(
                                    movableSharedElementHostState
                                )
                                .paneModifierConfiguration {
                                    Modifier.restrictedSizePlacement(
                                        atStart = paneState.pane == ThreePane.Secondary
                                    )
                                }
                                .animatePaneBoundsConfiguration(
                                    lookaheadScope = this@SharedTransitionScope,
                                    shouldAnimatePane = {
                                        when (paneState.pane) {
                                            ThreePane.Primary,
                                            ThreePane.Secondary,
                                            ThreePane.Tertiary -> !paneAnchorState.hasInteractions

                                            ThreePane.TransientPrimary -> true
                                            ThreePane.Overlay,
                                            null -> false
                                        }
                                    }
                                )
                        },
                    ) {
                        val filteredOrder by remember {
                            derivedStateOf { paneRenderOrder.filter { nodeFor(it) != null } }
                        }
                        splitLayoutState.visibleCount = filteredOrder.size
                        paneAnchorState.updateMaxWidth(
                            with(density) { splitLayoutState.size.roundToPx() }
                        )
                        SplitLayout(
                            state = splitLayoutState,
                            modifier = modifier
                                .fillMaxSize()
                                .then(sharedElementModifier)
                                .then(movableSharedElementHostState.modifier)
                                .routePanePadding(
                                    state = remember {
                                        derivedStateOf { listingAppState.globalUi.uiChromeState }
                                    }
                                )
                            ,
                            itemSeparators = { _, offset ->
                                DraggableThumb(
                                    splitLayoutState = splitLayoutState,
                                    paneAnchorState = paneAnchorState,
                                    offset = offset
                                )
                            },
                            itemContent = { index ->
                                DragToPopLayout(
                                    state = dragToDismissState,
                                    pane = filteredOrder[index]
                                )
                            }
                        )
                        LaunchedEffect(paneAnchorState.currentPaneAnchor) {
                            listingAppState.updateGlobalUi {
                                copy(paneAnchor = paneAnchorState.currentPaneAnchor)
                            }
                        }
                        LaunchedEffect(filteredOrder) {
                            if (filteredOrder.size != 1) return@LaunchedEffect
                            paneAnchorState.onClosed()
                        }
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
