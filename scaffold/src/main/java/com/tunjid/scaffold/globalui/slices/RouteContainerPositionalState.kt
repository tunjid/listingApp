package com.tunjid.scaffold.globalui.slices

import com.tunjid.scaffold.globalui.Ingress
import com.tunjid.scaffold.globalui.InsetDescriptor
import com.tunjid.scaffold.globalui.KeyboardAware
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.bottomNavVisible
import com.tunjid.scaffold.globalui.navRailVisible

internal data class RouteContainerPositionalState(
    val statusBarSize: Int,
    val toolbarOverlaps: Boolean,
    val navRailVisible: Boolean,
    val bottomNavVisible: Boolean,
    val windowSizeClass: WindowSizeClass,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal val UiState.routeContainerState
    get() = RouteContainerPositionalState(
        statusBarSize = systemUI.static.statusBarSize,
        insetDescriptor = insetFlags,
        toolbarOverlaps = toolbarOverlaps,
        bottomNavVisible = bottomNavVisible,
        navRailVisible = navRailVisible,
        windowSizeClass = windowSizeClass,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize
    )
