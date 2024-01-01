package com.tunjid.scaffold.globalui.slices

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.navRailVisible
import com.tunjid.scaffold.globalui.statusBarSize

internal data class ToolbarState(
    val statusBarSize: Int,
    val visible: Boolean,
    val overlaps: Boolean,
    val navRailVisible: Boolean,
    val toolbarTitle: CharSequence,
    val items: List<ToolbarItem>,
)

internal val UiState.toolbarState
    get() = ToolbarState(
        items = toolbarItems,
        toolbarTitle = toolbarTitle,
        visible = toolbarShows,
        overlaps = toolbarOverlaps,
        navRailVisible = navRailVisible,
        statusBarSize = statusBarSize,
    )

data class ToolbarItem(
    val id: String,
    val text: String,
    val imageVector: ImageVector? = null,
    val contentDescription: String? = null,
)
