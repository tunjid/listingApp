package com.tunjid.scaffold.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.composables.dragtodismiss.DragToDismissState
import com.tunjid.scaffold.adaptiveSpringSpec
import com.tunjid.scaffold.countIf
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.EXPANDED
import com.tunjid.scaffold.globalui.MEDIUM
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.bottomNavSize
import com.tunjid.scaffold.globalui.keyboardSize
import com.tunjid.scaffold.globalui.navRailWidth
import com.tunjid.scaffold.globalui.slices.UiChromeState
import com.tunjid.treenav.compose.Adaptation
import com.tunjid.treenav.compose.PanedNavHostScope
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.ThreePane.Companion.PrimaryToSecondary
import com.tunjid.treenav.compose.threepane.ThreePane.Companion.SecondaryToPrimary
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Motionally intelligent, adaptive pane for the hosting the navigation routes
 */
@Composable
internal fun PanedNavHostScope<ThreePane, Route>.ThreePaneLayout(
    uiChromeState: UiChromeState,
    onPaneAnchorChanged: (PaneAnchor) -> Unit,
) {
    val paddingValues = routePanePadding(uiChromeState)
    val (startClearance, topClearance, _, bottomClearance) = paddingValues
    val windowSizeClass = uiChromeState.windowSizeClass

    val hasSecondaryContent = nodeFor(ThreePane.Secondary) != null
    val density = LocalDensity.current
    val paneSplitState = remember { PaneAnchorState(density) }
    val dragToDismissState = remember { DragToDismissState() }

    CompositionLocalProvider(
        LocalPaneAnchorState provides paneSplitState,
        LocalDragToDismissState provides dragToDismissState,
    ) {
        Box(
            modifier = Modifier
                // Place under bottom navigation, app bars, fabs and the like
                .zIndex(PaneDragHandleZIndex)
                .fillMaxWidth()
                .onSizeChanged { paneSplitState.updateMaxWidth(it.width) }
                .padding(
                    start = startClearance,
                    top = topClearance,
                    bottom = bottomClearance
                ),
            content = {
                // Secondary pane content
                Box(
                    modifier = Modifier.secondaryPaneModifier(
                        adaptations = adaptationsIn(ThreePane.Secondary),
                        width = with(density) { paneSplitState.width.toDp() },
                        maxWidth = with(density) { paneSplitState.maxWidth.toDp() },
                    ),
                    content = {
                        Destination(ThreePane.Secondary)
                    }
                )
                // Primary pane content
                Box(
                    modifier = Modifier.primaryPaneModifier(
                        windowSizeClass = windowSizeClass,
                        adaptations = adaptationsIn(ThreePane.Primary),
                        secondaryContentWidth = with(density) { paneSplitState.width.toDp() },
                        maxWidth = with(density) { paneSplitState.maxWidth.toDp() }
                    ),
                    content = {
                        Box(
                            modifier = Modifier
                                .dragToPopInternal(dragToDismissState)
                        ) {
                            Destination(ThreePane.Primary)
                        }
                        // TODO: This should not be necessary. Figure out why a frame renders with
                        //  an offset of zero while the content in the transient primary container
                        //  is still visible.
                        val dragToDismissOffset by rememberUpdatedStateIf(
                            value = dragToDismissState.offset.round(),
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
                    }
                )
                // Pane separator
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = hasSecondaryContent
                            && windowSizeClass.minWidthDp > WindowSizeClass.COMPACT.minWidthDp
                ) {
                    DraggableThumb(
                        paneAnchorState = paneSplitState
                    )
                }
            }
        )
    }

    LaunchedEffect(windowSizeClass, hasSecondaryContent) {
        // Delay briefly so the animation runs
        delay(5)
        paneSplitState.moveTo(
            if (hasSecondaryContent) when (windowSizeClass.minWidthDp) {
                in WindowSizeClass.EXPANDED.minWidthDp..Int.MAX_VALUE -> PaneAnchor.Half
                in WindowSizeClass.MEDIUM.minWidthDp..<WindowSizeClass.EXPANDED.minWidthDp -> PaneAnchor.OneThirds
                else -> PaneAnchor.Zero
            }
            else PaneAnchor.Zero
        )
    }
    LaunchedEffect(paneSplitState.currentPaneAnchor) {
        onPaneAnchorChanged(paneSplitState.currentPaneAnchor)
    }
}

@Composable
private fun BoxScope.DraggableThumb(
    paneAnchorState: PaneAnchorState
) {
    val scope = rememberCoroutineScope()
    val isHovered by paneAnchorState.thumbInteractionSource.collectIsHoveredAsState()
    val isPressed by paneAnchorState.thumbInteractionSource.collectIsPressedAsState()
    val isDragged by paneAnchorState.thumbInteractionSource.collectIsDraggedAsState()
    val thumbWidth by animateDpAsState(
        if (isHovered || isPressed || isDragged) DraggableDividerSizeDp
        else when (paneAnchorState.targetPaneAnchor) {
            PaneAnchor.Zero -> DraggableDividerSizeDp
            PaneAnchor.OneThirds,
            PaneAnchor.Half,
            PaneAnchor.TwoThirds,
            PaneAnchor.Full -> 2.dp
        }
    )
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = paneAnchorState.width - (DraggableDividerSizeDp / 2).roundToPx(),
                    y = 0
                )
            }
            .align(Alignment.CenterStart)
            .width(DraggableDividerSizeDp)
            .then(paneAnchorState.modifier)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(thumbWidth)
                .height(DraggableDividerSizeDp),
            shape = RoundedCornerShape(DraggableDividerSizeDp),
            color = MaterialTheme.colorScheme.onSurface,
            onClick = {
                scope.launch { paneAnchorState.moveTo(PaneAnchor.OneThirds) }
            },
        ) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(degrees = 90f)
                    .scale(scale = 0.6f),
                imageVector = Icons.Filled.UnfoldMore,
                contentDescription = "Drag",
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

private fun Modifier.primaryPaneModifier(
    windowSizeClass: WindowSizeClass,
    adaptations: Set<Adaptation>,
    secondaryContentWidth: Dp,
    maxWidth: Dp,
) = composed {
    val updatedSecondaryContentWidth by rememberUpdatedState(secondaryContentWidth)
    val widthAnimatable = remember {
        Animatable(
            initialValue = maxWidth,
            typeConverter = Dp.VectorConverter,
            visibilityThreshold = Dp.VisibilityThreshold,
        )
    }
    var complete by remember { mutableStateOf(false) }

    LaunchedEffect(windowSizeClass, adaptations) {
        try {
            // Maintain max width on smaller devices
            if (windowSizeClass.minWidthDp <= WindowSizeClass.COMPACT.minWidthDp
                || !adaptations.contains(SecondaryToPrimary)
            ) {
                complete = true
                return@LaunchedEffect
            }
            complete = false
            // Snap to this width to give the impression of the pane sliding
            widthAnimatable.snapTo(targetValue = updatedSecondaryContentWidth)
            widthAnimatable.animateTo(
                targetValue = maxWidth,
                animationSpec = ContentSizeSpring,
            )
        } finally {
            complete = true
        }
    }

    Modifier
        .zIndex(PrimaryPaneZIndex)
        .width(if (complete) maxWidth else widthAnimatable.value)
        .padding(
            start = updatedSecondaryContentWidth.countIf(
                condition = !adaptations.contains(SecondaryToPrimary)
                        && windowSizeClass.minWidthDp > WindowSizeClass.COMPACT.minWidthDp
            )
        )
        .restrictedSizePlacement(
            atStart = adaptations.contains(SecondaryToPrimary)
        )
}

private fun Modifier.secondaryPaneModifier(
    adaptations: Set<Adaptation>,
    width: Dp,
    maxWidth: Dp,
) = composed {
    val updatedWidth = rememberUpdatedState(width)
    val updatedMaxWidth = rememberUpdatedState(maxWidth)
    val widthAnimatable = remember {
        Animatable(
            initialValue = maxWidth,
            typeConverter = Dp.VectorConverter,
            visibilityThreshold = Dp.VisibilityThreshold,
        )
    }
    var complete by remember { mutableStateOf(true) }

    LaunchedEffect(adaptations) {
        complete = !adaptations.contains(PrimaryToSecondary)
    }

    LaunchedEffect(complete) {
        if (complete) return@LaunchedEffect
        snapshotFlow { updatedWidth.value }
            .collectLatest { newestWidth ->
                // Don't cancel the previous animation, instead launch a new one and let the
                // animatable handle the retargeting required
                launch {
                    if (widthAnimatable.value != newestWidth) widthAnimatable.animateTo(
                        targetValue = newestWidth,
                        animationSpec = ContentSizeSpring,
                    )
                    complete = true
                    // Keep the animatable width at the full width for seamless animations
                    widthAnimatable.snapTo(targetValue = updatedMaxWidth.value)
                }
            }
    }

    Modifier
        // Display the secondary content over the primary content to maintain the sliding illusion
        .zIndex(if (complete) SecondaryPaneZIndex else SecondaryPaneAnimationZIndex)
        .width(if (complete) updatedWidth.value else widthAnimatable.value)
        .restrictedSizePlacement(
            atStart = adaptations.contains(PrimaryToSecondary)
        )
}

@Composable
private fun routePanePadding(
    state: UiChromeState,
): SnapshotStateList<Dp> {
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

    return paddingValues
}

/**
 * Shifts layouts out of view when the content area is too small instead of resizing them
 */
private fun Modifier.restrictedSizePlacement(
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

private val DraggableDividerSizeDp = 48.dp
private val MinPaneWidth = 120.dp

private val ContentSizeSpring = adaptiveSpringSpec(
    visibilityThreshold = Dp.VisibilityThreshold
)

private val PaneSizeSpring = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Dp.VisibilityThreshold
)

private const val PaneDragHandleZIndex = -1f
private const val PrimaryPaneZIndex = -2f
private const val SecondaryPaneZIndex = -3f
private const val SecondaryPaneAnimationZIndex = -1.5f