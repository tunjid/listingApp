package com.tunjid.feature.listinggallery.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.scaffold.adaptive.rememberSharedElement
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Media
import com.tunjid.scaffold.media.MediaArgs
import com.tunjid.tiler.compose.PivotedTilingEffect

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

    HorizontalPager(
        modifier = modifier
            .fillMaxSize()
            .dragToDismiss(
                onEnded = { actions(Action.Navigation.Pop) },
                onCancelled = { actions(Action.Navigation.Pop) }
            ),
        state = pagerState,
        key = { index -> state.items[index].url }
    ) { index ->
        val item = state.items[index]
        val thumbnail = rememberSharedElement<MediaArgs>(
            thumbnailSharedElementKey(item.url)
        ) { args, innerModifier ->
            Media(
                modifier = innerModifier,
                mediaArgs = args
            )
        }

        thumbnail(
            MediaArgs(
                url = item.url,
                contentScale = ContentScale.Fit
            ),
            Modifier
                .fillMaxSize(),
        )
    }

    // Paginate the gallery
    pagerState.PivotedTilingEffect(
        items = state.items,
        onQueryChanged = { query ->
            actions(Action.LoadImagesAround(query = query ?: state.currentQuery))
        }
    )
}
