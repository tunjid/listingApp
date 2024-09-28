package com.tunjid.scaffold.globalui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import java.util.UUID

sealed class NavMode {
    data object BottomNav : NavMode()
    data object NavRail : NavMode()
}

sealed class NavVisibility {
    data object Visible : NavVisibility()
    data object Gone : NavVisibility()
    data object GoneIfBottomNav : NavVisibility()
}

enum class PaneAnchor(
    val fraction: Float
) {
    Zero(fraction = 0f),
    OneThirds(fraction = 1 / 3f),
    Half(fraction = 1 / 2f),
    TwoThirds(fraction = 2 / 3f),
    Full(fraction = 1f),
}

/**
 * Message queue for notifying the UI according to the UI events guidance in the
 * [Android architecture docs](https://developer.android.com/jetpack/guide/ui-layer/events#handle-viewmodel-events)
 *
 */
data class MessageQueue(
    val items: List<Message> = listOf(),
)

data class Message(
    val value: String,
    val id: String = UUID.randomUUID().toString(),
)

fun MessageQueue.peek(): Message? = items.firstOrNull()

fun MessageQueue.filter(predicate: (Message) -> Boolean): MessageQueue = copy(
    items = items.filter(predicate)
)

operator fun MessageQueue.plus(message: Message): MessageQueue = copy(
    items = items + message
)

operator fun MessageQueue.plus(message: String): MessageQueue = copy(
    items = items + Message(value = message)
)

operator fun MessageQueue.minus(message: Message): MessageQueue = copy(
    items = items - message
)

data class UiState(
    val fabIcon: ImageVector = Icons.Default.Done,
    val fabShows: Boolean = false,
    val fabExtended: Boolean = true,
    val fabEnabled: Boolean = true,
    val fabText: String = "",
    val backgroundColor: Int = Color.Transparent.toArgb(),
    val snackbarOffset: Dp = 0.dp,
    val snackbarMessages: MessageQueue = MessageQueue(),
    val navBarColor: Int = Color.Transparent.toArgb(),
    val lightStatusBar: Boolean = false,
    val navMode: NavMode = NavMode.BottomNav,
    val navVisibility: NavVisibility = NavVisibility.Visible,
    val statusBarColor: Int = Color.Transparent.toArgb(),
    val insetFlags: InsetDescriptor = InsetFlags.ALL,
    val windowSizeClass: WindowSizeClass = WindowSizeClass.COMPACT,
    val isImmersive: Boolean = false,
    val systemUI: SystemUI = NoOpSystemUI,
    val backStatus: BackStatus = BackStatus.None,
    val paneAnchor: PaneAnchor = PaneAnchor.Zero,
    val fabClickListener: (Unit) -> Unit = emptyCallback(),
    val snackbarMessageConsumer: (Message) -> Unit = emptyCallback(),
)

private fun <T> emptyCallback(): (T) -> Unit = {}

val UiState.navBarSize get() = systemUI.static.navBarSize

val UiState.statusBarSize get() = systemUI.static.statusBarSize

val UiState.bottomNavVisible
    get() = navMode is NavMode.BottomNav && when (navVisibility) {
        NavVisibility.Visible -> true
        NavVisibility.Gone,
        NavVisibility.GoneIfBottomNav -> false
    }

val UiState.navRailVisible
    get() = navMode is NavMode.NavRail && when (navVisibility) {
        NavVisibility.Visible,
        NavVisibility.GoneIfBottomNav -> true

        NavVisibility.Gone -> false
    }

/**
 * Interface for [UiState] state slices that are aware of the keyboard. Useful for
 * keyboard visibility changes for bottom aligned views like Floating Action Buttons and Snack Bars
 */
interface KeyboardAware {
    val ime: Ingress
    val navBarSize: Int
    val insetDescriptor: InsetDescriptor
}

internal val KeyboardAware.keyboardSize get() = ime.bottom
