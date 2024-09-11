package com.tunjid.explore.pager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.min
import com.tunjid.scaffold.adaptive.movableSharedElementOf
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.dragToPop
import com.tunjid.scaffold.media.Video
import com.tunjid.scaffold.media.VideoState

@Composable
fun FullscreenGalleryScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            fabShows = false,
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NONE
        )
    )
    val isDebugging = state.isDebugging
    val updatedItems by rememberUpdatedState(newValue = state.items)
    val pagerState = rememberPagerState(
        initialPage = state.initialPage,
        pageCount = updatedItems::size
    )

    VerticalPager(
        modifier = modifier
            .fillMaxSize()
            .run {
                if (isDebugging) this
                else dragToPop()
            }
            .combinedClickable(
                onClick = { actions(Action.Navigation.Pop()) },
                onDoubleClick = { actions(Action.ToggleDebug) },
                onLongClick = { actions(Action.ToggleDebug) },
            ),
        state = pagerState,
        key = { index -> updatedItems[index].state.url },
    ) { index ->
        val item = updatedItems[index]
        DebugVideo(
            item = item,
            isDebugging = isDebugging
        )
    }

    LaunchedEffect(pagerState.currentPage) {
        actions(Action.Play(updatedItems[pagerState.currentPage].state.url))
    }
}


@Composable
private fun DebugVideo(
    item: GalleryItem,
    isDebugging: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        var width by remember(maxWidth) {
            mutableStateOf(maxWidth)
        }
        var height by remember(maxHeight) {
            mutableStateOf(maxHeight)
        }
        var transformOffset by remember {
            mutableStateOf(Offset.Zero)
        }

        LaunchedEffect(density, maxWidth, maxHeight, transformOffset) {
            width = min(
                a = maxWidth,
                b = width + with(density) { transformOffset.x.toDp() }
            )
            height = min(
                a = maxHeight,
                b = height + with(density) { transformOffset.y.toDp() }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(
                    animateDpAsState(
                        label = "Debug Video Width",
                        targetValue = if (isDebugging) width else maxWidth,
                    ).value
                )
                .height(
                    animateDpAsState(
                        label = "Debug Video Height",
                        targetValue = if (isDebugging) height else maxHeight,
                    ).value
                )
        ) {
            val transformableState = rememberTransformableState { _, offsetChange, _ ->
                transformOffset = offsetChange
            }
            val video = movableSharedElementOf<VideoState>(
                key = thumbnailSharedElementKey(item.state.url),
                sharedElement = { videoState, innerModifier ->
                    Video(
                        state = videoState,
                        modifier = innerModifier
                    )
                }
            )
            video(
                item.state,
                Modifier
                    .fillMaxSize()
                    .transformable(
                        state = transformableState,
                        enabled = isDebugging
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        animateColorAsState(
                            label = "Debug background",
                            targetValue = if (isDebugging) Color.Black.copy(alpha = 0.6f)
                            else Color.Transparent
                        ).value
                    )
            )
        }
    }
}

