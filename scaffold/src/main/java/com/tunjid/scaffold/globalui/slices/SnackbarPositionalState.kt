package com.tunjid.scaffold.globalui.slices

import com.tunjid.scaffold.globalui.Ingress
import com.tunjid.scaffold.globalui.InsetDescriptor
import com.tunjid.scaffold.globalui.KeyboardAware
import com.tunjid.scaffold.globalui.UiState
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.bottomNavVisible

internal data class SnackbarPositionalState(
    val bottomNavVisible: Boolean,
    val windowSizeClass: WindowSizeClass,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware


internal val UiState.snackbarPositionalState
    get() = SnackbarPositionalState(
        bottomNavVisible = bottomNavVisible,
        windowSizeClass = windowSizeClass,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )