package com.tunjid.scaffold.globalui.slices

import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.Ingress
import com.tunjid.scaffold.globalui.InsetDescriptor
import com.tunjid.scaffold.globalui.KeyboardAware
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.bottomNavVisible
import com.tunjid.scaffold.globalui.navRailVisible

internal data class UiChromeState(
    val statusBarSize: Int,
    val navRailVisible: Boolean,
    val bottomNavVisible: Boolean,
    val windowSizeClass: WindowSizeClass,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal val UiState.uiChromeState
    get() = UiChromeState(
        statusBarSize = systemUI.static.statusBarSize,
        insetDescriptor = insetFlags,
        bottomNavVisible = bottomNavVisible,
        navRailVisible = navRailVisible,
        windowSizeClass = windowSizeClass,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize
    )
