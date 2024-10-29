package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.round
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.scaffold.countIf
import com.tunjid.scaffold.globalui.bottomNavSize
import com.tunjid.scaffold.globalui.keyboardSize
import com.tunjid.scaffold.globalui.navRailWidth
import com.tunjid.scaffold.globalui.slices.UiChromeState
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route

@Composable
internal fun PanedNavHostScope<ThreePane, Route>.DragToPopLayout(
    state: DragToDismissState,
    pane: ThreePane,
) {
    if (pane == ThreePane.Primary) {
        Box(
            modifier = Modifier
                .dragToPopInternal(state)
        ) {
            Destination(pane)
        }
        // TODO: This should not be necessary. Figure out why a frame renders with
        //  an offset of zero while the content in the transient primary container
        //  is still visible.
        val dragToDismissOffset by rememberUpdatedStateIf(
            value = state.offset.round(),
            predicate = {
                it != IntOffset.Zero || nodeFor(ThreePane.TransientPrimary) == null
            }
        )
        Box(
            modifier = Modifier
                .offset { dragToDismissOffset }
        ) {
            Destination(ThreePane.TransientPrimary)
        }
    } else {
        Destination(pane)
    }
}

@Composable
internal fun Modifier.routePanePadding(
    state: UiChromeState,
): Modifier {
    val paddingValues = remember {
        mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp)
    }

    val bottomNavHeight = state.windowSizeClass.bottomNavSize() countIf state.bottomNavVisible

    val insetClearance = max(
        a = bottomNavHeight,
        b = with(LocalDensity.current) { state.keyboardSize.toDp() }
    )
    val navBarClearance = with(LocalDensity.current) {
        state.navBarSize.toDp()
    } countIf state.insetDescriptor.hasBottomInset

    val bottomClearance by animateDpAsState(
        targetValue = insetClearance + navBarClearance,
        animationSpec = PaneSizeSpring
    )

    val navRailSize = state.windowSizeClass.navRailWidth() countIf state.navRailVisible

    val startClearance by animateDpAsState(
        targetValue = navRailSize,
        animationSpec = PaneSizeSpring
    )

    paddingValues[0] = startClearance
    paddingValues[3] = bottomClearance

    return padding(
        start = paddingValues[0],
        top = paddingValues[1],
        end = paddingValues[2],
        bottom = paddingValues[3]
    )
}

/**
 * Shifts layouts out of view when the content area is too small instead of resizing them
 */
internal fun Modifier.restrictedSizePlacement(
    atStart: Boolean
) = layout { measurable, constraints ->
    val minPanWidth = MinPaneWidth.roundToPx()
    val actualConstraints = when {
        constraints.maxWidth < minPanWidth -> constraints.copy(maxWidth = minPanWidth)
        else -> constraints
    }
    val placeable = measurable.measure(actualConstraints)
    layout(width = placeable.width, height = placeable.height) {
        placeable.placeRelative(
            x = when {
                constraints.maxWidth < minPanWidth -> when {
                    atStart -> constraints.maxWidth - minPanWidth
                    else -> minPanWidth - constraints.maxWidth
                }

                else -> 0
            },
            y = 0
        )
    }
}

private val MinPaneWidth = 120.dp

private val PaneSizeSpring = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Dp.VisibilityThreshold
)
