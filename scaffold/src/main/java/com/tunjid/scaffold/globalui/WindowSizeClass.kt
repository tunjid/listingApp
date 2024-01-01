package com.tunjid.scaffold.globalui

import androidx.compose.ui.unit.dp

enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

fun WindowSizeClass.navRailWidth() =
    when (this) {
        WindowSizeClass.COMPACT -> 0.dp
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 72.dp
    }

fun WindowSizeClass.toolbarSize() =
    when (this) {
        WindowSizeClass.COMPACT -> 56.dp
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 72.dp
    }

fun WindowSizeClass.bottomNavSize() =
    when (this) {
        WindowSizeClass.COMPACT,
        WindowSizeClass.MEDIUM,
        WindowSizeClass.EXPANDED -> 80.dp
    }
