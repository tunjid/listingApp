package com.tunjid.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tunjid.feature.trips.R
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Video
import com.tunjid.scaffold.media.VideoArgs

@Composable
fun TripsScreen(
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
        modifier = Modifier.fillMaxSize(),
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

//    DebugVideo(url = state.videos.first())
        val gridState = rememberLazyGridState()

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = gridState,
        ) {
            items(
                items = state.videos,
                key = { it },
                itemContent = {
                    Video(
                        args = VideoArgs(
                            url = it,
                            contentScale = ContentScale.Crop,
                            isPlaying = false,
                        ),
                        modifier = Modifier.aspectRatio(9f / 16)
                    )
                }
            )
        }
    }
}

@Composable
private fun DebugVideo(
    url: String,
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
                args = VideoArgs(
                    url = url,
                    contentScale = ContentScale.Crop,
                ),
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