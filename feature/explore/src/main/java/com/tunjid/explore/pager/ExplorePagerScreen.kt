package com.tunjid.explore.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.tunjid.scaffold.adaptive.movableSharedElementOf
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
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
    val pagerState = rememberPagerState(pageCount = state.items::size)
    val playerManager = state.playerManager

    VerticalPager(
        modifier = modifier
            .fillMaxSize()
            .dragToDismiss(
                onDismissed = { actions(Action.Navigation.Pop()) }
            ),
        state = pagerState,
        key = { index -> state.items[index].url }
    ) { index ->
        val item = state.items[index]
        val video = movableSharedElementOf<VideoState>(
            thumbnailSharedElementKey(item.url)
        ) { videoState, innerModifier ->
            Video(
                modifier = innerModifier,
                state = videoState
            )
        }
        video(
            playerManager.stateFor(url = item.url),
            Modifier.fillMaxSize(),
        )
        LaunchedEffect(Unit) {
            println("play ${item.url} from launch; state: ${state.items.map { it.url }}")
            playerManager.play(item.url)
        }
    }
}
