package com.tunjid.scaffold.globalui.slices

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.tunjid.scaffold.globalui.Ingress
import com.tunjid.scaffold.globalui.InsetDescriptor
import com.tunjid.scaffold.globalui.KeyboardAware
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.globalui.bottomNavVisible

internal data class FabState(
    val fabVisible: Boolean,
    val bottomNavVisible: Boolean,
    val windowSizeClass: WindowSizeClass,
    val snackbarOffset: Dp,
    val icon: ImageVector,
    val extended: Boolean,
    val enabled: Boolean,
    val text: String,
    override val ime: Ingress,
    override val navBarSize: Int,
    override val insetDescriptor: InsetDescriptor
) : KeyboardAware

internal val UiState.fabState
    get() = FabState(
        fabVisible = fabShows,
        snackbarOffset = snackbarOffset,
        bottomNavVisible = bottomNavVisible,
        windowSizeClass = windowSizeClass,
        icon = fabIcon,
        text = fabText,
        extended = fabExtended,
        enabled = fabEnabled,
        ime = systemUI.dynamic.ime,
        navBarSize = systemUI.static.navBarSize,
        insetDescriptor = insetFlags
    )