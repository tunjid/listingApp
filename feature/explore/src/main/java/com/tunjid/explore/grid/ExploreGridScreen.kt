package com.tunjid.explore.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.adaptive.movableSharedElementOf
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Video
import com.tunjid.scaffold.media.VideoState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ExploreGridScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    ScreenUiState(
        UiState(
            fabShows = false,
            navVisibility = NavVisibility.Visible,
            insetFlags = InsetFlags.NONE
        )
    )

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            title = {
                Text(text = "Videos")
            }
        )

        val gridState = rememberLazyGridState()

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            state = gridState,
        ) {
            items(
                items = state.items,
                key = { it.key },
                itemContent = { item ->
                    // This box constraints the height of the container so the shared element does
                    // not push other items out of the way when animating in.
                    Box(
                        modifier = Modifier.aspectRatio(9f / 16)
                    ) {
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
                                .aspectRatio(9f / 16)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val url = item.state.url
                                    actions(Action.Play(url))
                                    actions(Action.Navigation.FullScreen(url))
                                }
                        )
                    }
                }
            )
        }

        val videos = state.items
        val index = gridState.visibleIndex(
            itemsAvailable = state.items.size
        )

        LaunchedEffect(index, videos) {
            if (index < 0) return@LaunchedEffect
            actions(Action.Play(videos[index].state.url))
        }
    }
}

@Composable
private fun DebugVideo(
    state: VideoState,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var widthRatio by remember {
            mutableFloatStateOf(0.4f)
        }
        var heightRatio by remember {
            mutableFloatStateOf(0.7f)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(widthRatio)
                .fillMaxHeight(heightRatio)
        ) {
            Video(
                state = state,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .width(24.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 0) {
                                widthRatio -= 0.01f
                            } else {
                                widthRatio += 0.01f
                            }
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .width(24.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 0) {
                                widthRatio += 0.01f
                            } else {
                                widthRatio -= 0.01f
                            }
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .height(24.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 0) {
                                heightRatio -= 0.01f
                            } else {
                                heightRatio += 0.01f
                            }
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .height(24.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 0) {
                                heightRatio += 0.01f
                            } else {
                                heightRatio -= 0.01f
                            }
                        }
                    }
            )
        }
    }
}

/**
 * Linearly interpolates the index for the first item in [visibleItems] for smooth scrollbar
 * progression.
 * @param visibleItems a list of items currently visible in the layout.
 * @param itemSize a lookup function for the size of an item in the layout.
 * @param offset a lookup function for the offset of an item relative to the start of the view port.
 * @param nextItemOnMainAxis a lookup function for the next item on the main axis in the direction
 * of the scroll.
 * @param itemIndex a lookup function for index of an item in the layout relative to
 * the total amount of items available.
 *
 * @return a [Float] in the range [firstItemPosition..nextItemPosition) where nextItemPosition
 * is the index of the consecutive item along the major axis.
 * */
internal inline fun <LazyState : ScrollableState, LazyStateItem> LazyState.interpolateFirstItemIndex(
    visibleItems: List<LazyStateItem>,
    crossinline itemSize: LazyState.(LazyStateItem) -> Int,
    crossinline offset: LazyState.(LazyStateItem) -> Int,
    crossinline nextItemOnMainAxis: LazyState.(LazyStateItem) -> LazyStateItem?,
    crossinline itemIndex: (LazyStateItem) -> Int,
): Float {
    if (visibleItems.isEmpty()) return 0f

    val firstItem = visibleItems.first()
    val firstItemIndex = itemIndex(firstItem)

    if (firstItemIndex < 0) return Float.NaN

    val firstItemSize = itemSize(firstItem)
    if (firstItemSize == 0) return Float.NaN

    val itemOffset = offset(firstItem).toFloat()
    val offsetPercentage = abs(itemOffset) / firstItemSize

    val nextItem = nextItemOnMainAxis(firstItem) ?: return firstItemIndex + offsetPercentage

    val nextItemIndex = itemIndex(nextItem)

    return firstItemIndex + ((nextItemIndex - firstItemIndex) * offsetPercentage)
}

@Composable
fun LazyGridState.visibleIndex(
    itemsAvailable: Int,
    itemIndex: (LazyGridItemInfo) -> Int = LazyGridItemInfo::index,
): Int {
    var position by remember {
        mutableIntStateOf(-1)
    }
    LaunchedEffect(this, itemsAvailable) {
        snapshotFlow {
            if (!isScrollInProgress) return@snapshotFlow -1
            if (itemsAvailable == 0) return@snapshotFlow -1

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow -1

            val firstIndex = min(
                a = interpolateFirstItemIndex(
                    visibleItems = visibleItemsInfo,
                    itemSize = { it.size.height },
                    offset = { it.offset.y },
                    nextItemOnMainAxis = { first ->
                        when (layoutInfo.orientation) {
                            Orientation.Vertical -> visibleItemsInfo.find {
                                it != first && it.row != first.row
                            }

                            Orientation.Horizontal -> visibleItemsInfo.find {
                                it != first && it.column != first.column
                            }
                        }
                    },
                    itemIndex = itemIndex,
                ),
                b = itemsAvailable.toFloat(),
            )

            if (firstIndex.isNaN()) -1 else firstIndex.roundToInt()
        }
            .distinctUntilChanged()
            .collect { position = it }
    }
    return position
}