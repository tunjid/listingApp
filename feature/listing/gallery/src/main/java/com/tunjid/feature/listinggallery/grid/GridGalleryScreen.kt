package com.tunjid.feature.listinggallery.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
fun GridGalleryScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarShows = true,
            fabShows = false,
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NO_BOTTOM
        )
    )

    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        state = gridState,
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
            horizontal = 2.dp,
            vertical = 2.dp,
        )
    ) {
        items(
            items = state.items,
            key = GalleryItem::url,
            itemContent = { item ->
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
                        contentScale = ContentScale.Crop
                    ),
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable {
                            if (item is GalleryItem.Loaded) actions(
                                Action.Navigation.FullScreen(
                                    listingId = item.image.listingId,
                                    url = item.url,
                                )
                            )
                        },
                )
            }
        )
    }

    // Paginate the gallery
    gridState.PivotedTilingEffect(
        items = state.items,
        onQueryChanged = { query ->
            actions(Action.LoadImagesAround(query = query ?: state.currentQuery))
        }
    )
}