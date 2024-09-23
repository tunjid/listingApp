package com.tunjid.scaffold.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tunjid.treenav.adaptive.AdaptiveNavHostState
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.strings.Route

@Stable
interface AdaptiveContentState: AdaptiveNavHostState<ThreePane, Route> {


    val overlays: Collection<SharedElementOverlay>

}
