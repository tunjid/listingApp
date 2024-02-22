package com.tunjid.feature.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.listing.feature.listing.feed.R
import com.tunjid.listing.sync.SyncStatus
import com.tunjid.scaffold.adaptive.sharedElementOf
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.media.Media
import com.tunjid.scaffold.media.MediaArgs
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun ListingFeedScreen(
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
    val gridState = rememberLazyGridState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = {
            actions(Action.Refresh)
        }
    )

    Box(
        modifier = modifier.pullRefresh(
            state = pullRefreshState
        )
    ) {
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
                    Text(text = stringResource(id = R.string.listing_app))
                }
            )
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
            ) {
                items(
                    items = state.listings,
                    key = FeedItem::id,
                    span = {
                        actions(Action.LoadFeed.GridSize(maxLineSpan))
                        GridItemSpan(currentLineSpan = 1)
                    },
                    itemContent = { feedItem ->
                        FeedItemCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                            feedItem = feedItem,
                            actions = actions,
                        )
                    }
                )
            }
        }
        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(64.dp)
        )
        EmptyView(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            status = state.syncStatus,
            isEmpty = state.listings.isEmpty()
        )
    }

    gridState.PivotedTilingEffect(
        items = state.listings,
        onQueryChanged = { query ->
            actions(Action.LoadFeed.LoadAround(query ?: state.currentQuery))
        }
    )
}

@Composable
private fun FeedItemCard(
    modifier: Modifier = Modifier,
    feedItem: FeedItem,
    actions: (Action) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        content = {
            val pagerState = rememberPagerState(pageCount = feedItem.medias::size)
            Card(
                modifier = Modifier,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                ) {
                    FeedMediaPager(
                        pagerState = pagerState,
                        feedItem = feedItem,
                        actions = actions
                    )
                    if (feedItem.medias.isNotEmpty()) MaximizePager(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        listingId = feedItem.listing.id,
                        url = feedItem.medias[pagerState.currentPage].url,
                        actions = actions,
                    )
                    FavoriteButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        listingId = feedItem.listing.id,
                        isFavorite = feedItem.isFavorite,
                        actions = actions,
                    )
                }
            }
            FeedItemInfo(
                feedItem = feedItem
            )
        }
    )
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier,
    status: SyncStatus,
    isEmpty: Boolean
) {
    Box(
        modifier = modifier,
    ) {
        when (status) {
            SyncStatus.Running -> Unit
            SyncStatus.Idle,
            SyncStatus.Success,
            SyncStatus.Cancelled -> if (isEmpty) {
                Text(text = stringResource(id = R.string.no_homes))
            }

            SyncStatus.Failure -> if (isEmpty) {
                Text(text = stringResource(id = R.string.search_error))
            }
        }
    }
}

@Composable
private fun FeedMediaPager(
    pagerState: PagerState,
    feedItem: FeedItem,
    actions: (Action) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        key = { index -> feedItem.medias[index].url }
    ) { index ->
        val media = feedItem.medias[index]
        val thumbnail = sharedElementOf<MediaArgs>(
            thumbnailSharedElementKey(media.url)
        ) { args, innerModifier ->
            Media(
                modifier = innerModifier,
                mediaArgs = args
            )
        }
        thumbnail(
            MediaArgs(
                url = media.url,
                contentScale = ContentScale.Crop
            ),
            Modifier
                .fillMaxSize()
                .clickable {
                    actions(
                        Action.Navigation.Detail(
                            listingId = feedItem.listing.id,
                            url = media.url
                        )
                    )
                },
        )
    }
}

@Composable
private fun FeedItemInfo(feedItem: FeedItem) {
    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        Text(
            text = feedItem.listing.title,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            text = feedItem.listing.address,
            fontSize = 12.sp
        )
        Text(
            text = feedItem.listing.price,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
private fun MaximizePager(
    modifier: Modifier = Modifier,
    listingId: String,
    url: String,
    actions: (Action) -> Unit
) {
    FilledTonalIconButton(
        modifier = modifier,
        onClick = {
            actions(
                Action.Navigation.Gallery(
                    listingId = listingId,
                    url = url
                )
            )
        }
    ) {
        Image(
            modifier = Modifier
                .rotate(degrees = 45f),
            imageVector = Icons.Filled.UnfoldMore,
            contentDescription = "Expand",
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun FavoriteButton(
    modifier: Modifier = Modifier,
    listingId: String,
    isFavorite: Boolean,
    actions: (Action) -> Unit
) {
    FilledTonalIconButton(
        modifier = modifier,
        onClick = {
            actions(
                Action.SetFavorite(
                    listingId = listingId,
                    isFavorite = !isFavorite
                )
            )
        }
    ) {
        Image(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Favorite",
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
