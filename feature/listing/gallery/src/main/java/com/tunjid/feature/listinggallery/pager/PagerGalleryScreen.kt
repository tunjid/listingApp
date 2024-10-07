package com.tunjid.feature.listinggallery.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.media.Photo
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.scaffold.dragToPop
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.MovableSharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun FullscreenGalleryScreen(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = state.items::size)

    HorizontalPager(
        modifier = modifier
            .fillMaxSize()
            .dragToPop(),
        state = pagerState,
        key = { index -> state.items[index].url }
    ) { index ->
        val item = state.items[index]
        val thumbnail = movableSharedElementScope.movableSharedElementOf<PhotoArgs>(
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
