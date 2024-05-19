package com.tunjid.scaffold.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface AdaptiveContentState {

    val navigationState: Adaptive.NavigationState

    val overlays: Collection<SharedElementOverlay>

    @Composable
    fun RouteIn(pane: Adaptive.Pane)
}
