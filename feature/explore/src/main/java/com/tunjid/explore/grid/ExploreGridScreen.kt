package com.tunjid.explore.grid

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tunjid.composables.lazy.grid.interpolatedFirstItemIndex
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.media.PlayerStatus
import com.tunjid.scaffold.media.Video
import com.tunjid.scaffold.media.VideoState
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ExploreGridScreen(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
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
        val updatedItems by rememberUpdatedState(state.items)

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            state = gridState,
        ) {
            items(
                items = updatedItems,
                key = { it.key },
                itemContent = { item ->
                    // This box constraints the height of the container so the shared element does
                    // not push other items out of the way when animating in.
                    Box(
                        modifier = Modifier
                            .aspectRatio(9f / 16)
                            .animateItem()
                    ) {
                        movableSharedElementScope.updatedMovableSharedElementOf(
                            key = thumbnailSharedElementKey(item.state.url),
                            state = item.state,
                            modifier = Modifier
                                .aspectRatio(9f / 16)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val url = item.state.url
                                    actions(Action.Play(url))
                                    actions(
                                        Action.Navigation.FullScreen(
                                            startingUrl = url,
                                            urls = updatedItems.map { it.state.url }
                                        )
                                    )
                                },
                            alternateOutgoingSharedElement = { videoState, innerModifier ->
                                videoState.videoStill?.let {
                                    Image(
                                        bitmap = it,
                                        modifier = innerModifier,
                                        contentDescription = null,
                                    )
                                }
                            },
                            sharedElement = { videoState, innerModifier ->
                                Video(
                                    state = videoState,
                                    modifier = innerModifier
                                )
                            }
                        )
                    }
                }
            )
        }

        val index = gridState.visibleIndex(
            itemsAvailable = updatedItems.size
        )

        LaunchedEffect(index) {
            if (index < 0) return@LaunchedEffect
            actions(Action.Play(updatedItems[index].state.url))
        }

        // Scroll to the playing item when entering the screen for the first time
        LaunchedEffect(gridState) {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@LaunchedEffect

            val playingIndex = updatedItems.indexOfFirst { it.state.status is PlayerStatus.Play }
            if (playingIndex in visibleItems.first().index..visibleItems.last().index) {
                return@LaunchedEffect
            }
            val indexToScrollTo = if (playingIndex >= 0) playingIndex else 0

            gridState.scrollToItem(indexToScrollTo)
            actions(Action.Play(updatedItems[indexToScrollTo].state.url))
        }
    }
}

@Composable
fun LazyGridState.visibleIndex(
    itemsAvailable: Int,
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
                a = interpolatedFirstItemIndex(),
                b = itemsAvailable.toFloat(),
            )

            if (firstIndex.isNaN()) -1 else firstIndex.roundToInt()
        }
            .distinctUntilChanged()
            .collect { position = it }
    }
    return position
}