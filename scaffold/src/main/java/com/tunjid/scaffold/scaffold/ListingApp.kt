package com.tunjid.scaffold.scaffold

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import com.tunjid.composables.splitlayout.SplitLayout
import com.tunjid.composables.splitlayout.SplitLayoutState
import com.tunjid.composables.ui.skipIf
import com.tunjid.scaffold.globalui.slices.bottomNavPositionalState
import com.tunjid.scaffold.globalui.slices.fabState
import com.tunjid.scaffold.globalui.slices.snackbarPositionalState
import com.tunjid.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.scaffold.PaneAnchorState.Companion.DraggableThumb
import com.tunjid.scaffold.scaffold.PaneAnchorState.Companion.MinPaneWidth
import com.tunjid.scaffold.scaffold.configuration.predictiveBackConfiguration
import com.tunjid.treenav.compose.PanedNavHost
import com.tunjid.treenav.compose.configurations.animatePaneBoundsConfiguration
import com.tunjid.treenav.compose.configurations.paneModifierConfiguration
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementHostState
import com.tunjid.treenav.compose.threepane.ThreePane
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
            minSize = MinPaneWidth,
            keyAtIndex = { index ->
                val indexDiff = paneRenderOrder.size - visibleCount
                paneRenderOrder[index + indexDiff]
            }
        )
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppState provides listingAppState,
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
                        MovableSharedElementHostState<ThreePane, Route>(
                            sharedTransitionScope = this@SharedTransitionScope,
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
                                    paneBoundsTransform = {
                                        BoundsTransform { _, _ ->
                                            spring<Rect>().skipIf {
                                                when (paneState.pane) {
                                                    ThreePane.Primary,
                                                    ThreePane.Secondary,
                                                    ThreePane.Tertiary -> !listingAppState.paneAnchorState.hasInteractions

                                                    ThreePane.TransientPrimary -> true
                                                    ThreePane.Overlay,
                                                    null -> false
                                                }
                                            }
                                        }
                                    }
                                )
                        },
                    ) {
                        val filteredOrder by remember {
                            derivedStateOf { paneRenderOrder.filter { nodeFor(it) != null } }
                        }
                        splitLayoutState.visibleCount = filteredOrder.size
                        listingAppState.paneAnchorState.updateMaxWidth(
                            with(density) { splitLayoutState.size.roundToPx() }
                        )
                        SplitLayout(
                            state = splitLayoutState,
                            modifier = modifier
                                .fillMaxSize()
                                .then(sharedElementModifier)
                                .routePanePadding(
                                    state = remember {
                                        derivedStateOf { listingAppState.globalUi.uiChromeState }
                                    }
                                ),
                            itemSeparators = { _, offset ->
                                DraggableThumb(
                                    splitLayoutState = splitLayoutState,
                                    paneAnchorState = listingAppState.paneAnchorState,
                                    offset = offset
                                )
                            },
                            itemContent = { index ->
                                DragToPopLayout(
                                    state = listingAppState,
                                    pane = filteredOrder[index]
                                )
                            }
                        )
                        LaunchedEffect(listingAppState.paneAnchorState.currentPaneAnchor) {
                            listingAppState.updateGlobalUi {
                                copy(paneAnchor = listingAppState.paneAnchorState.currentPaneAnchor)
                            }
                        }
                        LaunchedEffect(filteredOrder) {
                            if (filteredOrder.size != 1) return@LaunchedEffect
                            listingAppState.paneAnchorState.onClosed()
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
