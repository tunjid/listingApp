package com.tunjid.feature.listinggallery.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Photo
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.scaffold.dragToPop
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.movableSharedElementOf
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
            .dragToPop(),
        state = pagerState,
        key = { index -> state.items[index].url }
    ) { index ->
        val item = state.items[index]
        val thumbnail = movableSharedElementOf<PhotoArgs>(
            thumbnailSharedElementKey(item.url)
        ) { args, innerModifier ->
            Photo(
                modifier = innerModifier,
                args = args
            )
        }

        thumbnail(
            PhotoArgs(
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
