package com.tunjid.scaffold.globalui.slices

import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.InsetDescriptor
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.bottomNavVisible

internal data class BottomNavPositionalState(
    val insetDescriptor: InsetDescriptor,
    val bottomNavVisible: Boolean,
    val windowSizeClass: WindowSizeClass,
    val navBarSize: Int
)

internal val UiState.bottomNavPositionalState
    get() = BottomNavPositionalState(
        bottomNavVisible = bottomNavVisible,
        navBarSize = systemUI.static.navBarSize,
        windowSizeClass = windowSizeClass,
        insetDescriptor = insetFlags
    )