package com.tunjid.feature.listinggallery.grid

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.media.Photo
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.scaffold.PaneScaffoldState
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf

@Composable
fun GridGalleryScreen(
    paneScaffoldState: PaneScaffoldState,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    val gridState = rememberLazyGridState()
    Column(modifier = modifier.fillMaxSize()) {
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
                    // This box constraints the height of the container so the shared element does
                    // not push other items out of the way when animating in.
                    Box(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = paneScaffoldState,
                                boundsTransform = { _, _ ->
                                    spring(stiffness = Spring.StiffnessMedium)
                                }
                            )
                            .aspectRatio(1f)
                            .animateItem()
                    ) {
                        paneScaffoldState.updatedMovableSharedElementOf(
                            key = thumbnailSharedElementKey(item.url),
                            state = PhotoArgs(
                                url = item.url,
                                contentScale = ContentScale.Crop
                            ),
                            modifier = Modifier
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
                            sharedElement = { state, innerModifier ->
                                Photo(
                                    modifier = innerModifier,
                                    args = state
                                )
                            }
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