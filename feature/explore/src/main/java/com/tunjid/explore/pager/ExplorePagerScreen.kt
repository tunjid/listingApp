package com.tunjid.explore.pager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
    val updatedItems by rememberUpdatedState(newValue = state.items)
    val pagerState = rememberPagerState(
        initialPage = state.initialPage,
        pageCount = updatedItems::size
    )

    VerticalPager(
        modifier = modifier
            .fillMaxSize()
            .dragToPop()
            .clickable {
                actions(Action.Navigation.Pop())
            },
        state = pagerState,
        key = { index -> updatedItems[index].state.url },
    ) { index ->
        val item = updatedItems[index]
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
            Modifier.fillMaxSize(),
        )
    }

    LaunchedEffect(pagerState.currentPage) {
        actions(Action.Play(updatedItems[pagerState.currentPage].state.url))
    }
}
