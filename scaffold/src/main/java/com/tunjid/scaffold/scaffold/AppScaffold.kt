package com.tunjid.scaffold.scaffold

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.adaptive.Adaptive
import com.tunjid.scaffold.adaptive.AdaptiveContentRoot
import com.tunjid.scaffold.adaptive.AdaptiveContentState
import com.tunjid.scaffold.adaptive.LocalAdaptiveContentScope
import com.tunjid.scaffold.globalui.BackStatus
import com.tunjid.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.scaffold.globalui.LocalGlobalUiStateHolder
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.isFromLeft
import com.tunjid.scaffold.globalui.progress
import com.tunjid.scaffold.globalui.slices.uiChromeState
import com.tunjid.scaffold.globalui.touchX
import com.tunjid.scaffold.globalui.touchY
import com.tunjid.scaffold.lifecycle.mappedCollectAsStateWithLifecycle
import com.tunjid.scaffold.navigation.LocalNavigationStateHolder
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.treenav.adaptive.AdaptiveNavHost
import kotlin.math.roundToInt

/**
 * Root scaffold for the app
 */
@Composable
fun Scaffold(
    modifier: Modifier,
    adaptiveContentState: AdaptiveContentState,
    navStateHolder: NavigationStateHolder,
    globalUiStateHolder: GlobalUiStateHolder,
) {
    CompositionLocalProvider(
        LocalGlobalUiStateHolder provides globalUiStateHolder,
        LocalNavigationStateHolder provides navStateHolder,
    ) {
        Surface {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                AppNavRail(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                // Root LookaheadScope used to anchor all shared element transitions
//                AdaptiveContentRoot(adaptiveContentState) {
                    AdaptiveNavHost(
                        state = adaptiveContentState,
                        modifier = modifier.fillMaxSize()
                    ) {
                        AdaptiveContentScaffold(
                            positionalState = globalUiStateHolder.state.mappedCollectAsStateWithLifecycle(
                                mapper = UiState::uiChromeState
                            ).value,
                            onPaneAnchorChanged = remember {
                                { paneAnchor: PaneAnchor ->
                                    globalUiStateHolder.accept {
                                        copy(paneAnchor = paneAnchor)
                                    }
                                }
                            },
                        )
                    }
//                }
                AppFab(
                    globalUiStateHolder = globalUiStateHolder,
                )
                AppBottomNav(
                    globalUiStateHolder = globalUiStateHolder,
                    navStateHolder = navStateHolder,
                )
                AppSnackBar(
                    globalUiStateHolder = globalUiStateHolder,
                )
            }
        }
    }
}

// Previews back content as specified by the material motion spec for Android predictive back:
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs
internal fun Modifier.backPreviewModifier(): Modifier =
    composed {
        val configuration = LocalConfiguration.current
        val globalUiStateHolder = LocalGlobalUiStateHolder.current

        val uiStateFlow = remember {
            globalUiStateHolder.state
        }
        val backStatus by uiStateFlow.mappedCollectAsStateWithLifecycle(
            mapper = UiState::backStatus
        )
        if (backStatus is BackStatus.DragDismiss) {
            return@composed this
        }
        val scale by animateFloatAsState(
            // Deviates from the spec here. The spec says 90% of the pane, I'm doing 85%
            targetValue = 1f - (backStatus.progress * 0.15F),
            label = "back preview modifier scale"
        )
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = (constraints.maxWidth * scale).roundToInt(),
                        minWidth = (constraints.minWidth * scale).roundToInt(),
                        maxHeight = (constraints.maxHeight * scale).roundToInt(),
                        minHeight = (constraints.minHeight * scale).roundToInt(),
                    )
                )
                val paneWidth = placeable.width
                val paneHeight = placeable.height

                val scaledWidth = paneWidth * scale
                val spaceOnEachSide = (paneWidth - scaledWidth) / 2
                val margin = (BACK_PREVIEW_PADDING * backStatus.progress).dp.roundToPx()

                val xOffset = ((spaceOnEachSide - margin) * when {
                    backStatus.isFromLeft -> 1
                    else -> -1
                }).toInt()

                val maxYShift = ((paneHeight / 20) - BACK_PREVIEW_PADDING)
                val isOrientedHorizontally = paneWidth > paneHeight
                val screenSize = when {
                    isOrientedHorizontally -> configuration.screenWidthDp
                    else -> configuration.screenHeightDp
                }.dp.roundToPx()
                val touchPoint = when {
                    isOrientedHorizontally -> backStatus.touchX
                    else -> backStatus.touchY
                }
                val verticalProgress = (touchPoint / screenSize) - 0.5f
                val yOffset = (verticalProgress * maxYShift).roundToInt()

                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x = xOffset, y = yOffset)
                }
            }
            // Disable interactions in the preview pane
            .pointerInput(Unit) {}
    }

fun Modifier.backPreviewBackgroundModifier(): Modifier = composed {
    val scope = LocalAdaptiveContentScope.current
    if (scope?.paneState?.pane != Adaptive.Pane.TransientPrimary)
        return@composed this

    var elevation by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
        ) { value, _ -> elevation = value.dp }
    }
    background(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
        shape = RoundedCornerShape(16.dp)
    )
        .clip(RoundedCornerShape(16.dp))

}

private const val BACK_PREVIEW_PADDING = 8

