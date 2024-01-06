package com.tunjid.feature.listinggallery.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tunjid.listing.feature.listing.gallery.R
import com.tunjid.scaffold.adaptive.rememberSharedContent
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Media
import com.tunjid.scaffold.media.MediaArgs
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun GridGalleryScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarTitle = stringResource(R.string.gallery),
            toolbarShows = false,
            toolbarOverlaps = true,
            fabShows = false,
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NONE
        )
    )

    val gridState = rememberLazyGridState()
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars),
            title = {
                Text(text = stringResource(id = R.string.gallery))
            }
        )
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = GridCells.Adaptive(GridItemSize),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                horizontal = 2.dp,
                vertical = 2.dp,
            )
        ) {
            items(
                items = state.items,
                key = GalleryItem::url,
                span = {
                    actions(Action.LoadItems.GridSize(maxLineSpan))
                    GridItemSpan(currentLineSpan = 1)
                },
                itemContent = { item ->
                    Box(
                        modifier = Modifier.heightIn(max = GridItemSize)
                    ) {
                        val thumbnail = rememberSharedContent<MediaArgs>(
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
                                contentScale = ContentScale.Crop
                            ),
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable {
                                    if (item is GalleryItem.Loaded) actions(
                                        Action.Navigation.FullScreen(
                                            listingId = item.media.listingId,
                                            url = item.url,
                                        )
                                    )
                                },
                        )
                    }
                }
            )
        }
    }

    // Paginate the gallery
    gridState.PivotedTilingEffect(
        items = state.items,
        onQueryChanged = { query ->
            actions(Action.LoadItems.Around(query = query ?: state.currentQuery))
        }
    )
}

private val GridItemSize = 160.dp