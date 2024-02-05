package com.tunjid.scaffold.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.layout.LookaheadScope
import com.tunjid.scaffold.di.AdaptiveRouter
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.scaffold.SavedStateAdaptiveContentState
import com.tunjid.treenav.MultiStackNav
import kotlinx.coroutines.flow.StateFlow

@Stable
interface AdaptiveContentState {

    val navigationState: Adaptive.NavigationState

    @Composable
    fun RouteIn(container: Adaptive.Container?)
}
