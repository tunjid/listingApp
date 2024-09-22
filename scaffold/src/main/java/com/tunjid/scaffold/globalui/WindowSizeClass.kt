package com.tunjid.scaffold.globalui

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

val WindowSizeClass.Companion.COMPACT get() = WINDOW_SIZE_CLASS_COMPACT

val WindowSizeClass.Companion.MEDIUM get() = WINDOW_SIZE_CLASS_MEDIUM

val WindowSizeClass.Companion.EXPANDED get() = WINDOW_SIZE_CLASS_EXPANDED

fun WindowSizeClass.navRailWidth() =
    when (minWidthDp) {
        in WindowSizeClass.MEDIUM.minWidthDp..Int.MAX_VALUE -> 72.dp
        else -> 0.dp
    }

@Suppress("UnusedReceiverParameter")
fun WindowSizeClass.bottomNavSize() = 80.dp

private val WINDOW_SIZE_CLASS_COMPACT = WindowSizeClass(
    minWidthDp = 0,
    minHeightDp = 0,
)

private val WINDOW_SIZE_CLASS_MEDIUM = WindowSizeClass(
    minWidthDp = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    minHeightDp = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
)

private val WINDOW_SIZE_CLASS_EXPANDED = WindowSizeClass(
    minWidthDp = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    minHeightDp = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND,
)