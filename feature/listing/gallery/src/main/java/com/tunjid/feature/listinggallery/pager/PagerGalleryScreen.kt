package com.tunjid.feature.listinggallery.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tunjid.scaffold.ImageArgs
import com.tunjid.scaffold.adaptive.rememberSharedContent
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun FullscreenGalleryScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarShows = false,
            toolbarOverlaps = true,
            fabShows = false,
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NONE
        )
    )
    val pagerState = rememberPagerState(pageCount = state.items::size)

    HorizontalPager(
        modifier = modifier
            .fillMaxSize(),
        state = pagerState,
        key = { index -> state.items[index].url }
    ) { index ->
        val item = state.items[index]
        val thumbnail = rememberSharedContent<ImageArgs>(
            thumbnailSharedElementKey(item.url)
        ) { args, innerModifier ->
            AsyncImage(
                modifier = innerModifier,
                model = args.url,
                contentDescription = null,
                contentScale = args.contentScale
            )
        }

        thumbnail(
            ImageArgs(
                url = item.url,
                contentScale = ContentScale.Fit
            ),
            Modifier.fillMaxSize(),
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