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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.BackStatus
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.isFromLeft
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.globalui.progress
import com.tunjid.scaffold.globalui.touchX
import com.tunjid.scaffold.globalui.touchY
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.adaptive.AdaptiveNavHostConfiguration
import com.tunjid.treenav.adaptive.AdaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.AdaptivePaneScope
import com.tunjid.treenav.adaptive.adaptiveNodeConfiguration
import com.tunjid.treenav.adaptive.delegated
import com.tunjid.treenav.adaptive.paneMapping
import com.tunjid.treenav.adaptive.threepane.ThreePane
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route
import kotlin.math.roundToInt

fun AdaptiveNavHostConfiguration<ThreePane, MultiStackNav, Route>.predictiveBackConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
    backStatusState: State<BackStatus>,
) = delegated(
    currentNode = derivedStateOf {
        val current = currentNode.value
        if (backStatusState.value.isPreviewing) navigationState.value.pop().current as Route
        else current
    },
    configuration = { node ->
        val originalConfiguration = configuration(node)
        adaptiveNodeConfiguration(
            transitions = originalConfiguration.transitions,
            paneMapping = paneMapper@{ inner ->
                val originalMapping = originalConfiguration.paneMapper(inner)
                val isPreviewingBack by remember {
                    derivedStateOf { backStatusState.value.isPreviewing }
                }
                if (!isPreviewingBack) return@paneMapper originalMapping

                // Back is being previewed, therefore the original mapping is already for back.
                // Pass the previous primary value into transient.
                val transient = this@predictiveBackConfiguration.paneMapping()[ThreePane.Primary]
                originalMapping + (ThreePane.TransientPrimary to transient)
            },
            render = paneScope@{ toRender ->
                val windowSizeClass by windowSizeClassState
                val backStatus by backStatusState
                Box(
                    modifier = Modifier.adaptiveModifier(
                        windowSizeClass = windowSizeClass,
                        backStatus = backStatus,
                        nodeConfiguration = originalConfiguration,
                        adaptivePaneScope = this@paneScope
                    )
                )
                {
                    originalConfiguration.render.invoke(this@paneScope, toRender)
                }
            }
        )
    })

@Composable
private fun Modifier.adaptiveModifier(
    windowSizeClass: WindowSizeClass,
    backStatus: BackStatus,
    nodeConfiguration: AdaptiveNodeConfiguration<ThreePane, Route>,
    adaptivePaneScope: AdaptivePaneScope<ThreePane, Route>,
): Modifier = this then with(adaptivePaneScope) {
    when (paneState.pane) {
        ThreePane.Primary, ThreePane.Secondary -> FillSizeModifier
            .background(color = MaterialTheme.colorScheme.surface)
            .run {
                if (windowSizeClass.minWidthDp <= WindowSizeClass.COMPACT.minWidthDp) this
                else clip(RoundedCornerShape(16.dp))
            }
            .run {
                val enterAndExit = nodeConfiguration.transitions(adaptivePaneScope)
                animateEnterExit(
                    enter = enterAndExit.enter,
                    exit = enterAndExit.exit
                )
            }

        ThreePane.TransientPrimary -> FillSizeModifier.predictiveBackModifier(
            backStatus = backStatus
        )

        else -> FillSizeModifier
    }
}

private val FillSizeModifier = Modifier.fillMaxSize()

// Previews back content as specified by the material motion spec for Android predictive back:
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs
@Composable
private fun Modifier.predictiveBackModifier(
    backStatus: BackStatus
): Modifier {
    val configuration = LocalConfiguration.current

    if (backStatus is BackStatus.DragDismiss) {
        return this
    }
    val scale by animateFloatAsState(
        // Deviates from the spec here. The spec says 90% of the pane, I'm doing 85%
        targetValue = 1f - (backStatus.progress * 0.15F),
        label = "back preview modifier scale"
    )
    return layout { measurable, constraints ->
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

@Composable
fun Modifier.predictiveBackBackgroundModifier(
    paneScope: AdaptivePaneScope<ThreePane, *>
): Modifier {
    if (paneScope.paneState.pane != ThreePane.TransientPrimary)
        return this

    var elevation by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
        ) { value, _ -> elevation = value.dp }
    }
    return background(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
        shape = RoundedCornerShape(16.dp)
    )
        .clip(RoundedCornerShape(16.dp))

}

private const val BACK_PREVIEW_PADDING = 8