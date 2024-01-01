package com.tunjid.feature.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tunjid.data.image.Image
import com.tunjid.listing.feature.listing.feed.R
import com.tunjid.listing.sync.SyncStatus
import com.tunjid.scaffold.ImageArgs
import com.tunjid.scaffold.adaptive.rememberSharedContent
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun ListingFeedScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    ScreenUiState(
        UiState(
            toolbarTitle = stringResource(id = R.string.listing_app),
            toolbarShows = true,
            fabShows = false,
            navVisibility = NavVisibility.Visible,
            insetFlags = InsetFlags.NO_BOTTOM
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
                    actions(Action.List.GridSize(maxLineSpan))
                    GridItemSpan(1)
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
        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        EmptyView(
            status = state.syncStatus,
            isEmpty = state.listings.isEmpty()
        )
    }

    gridState.PivotedTilingEffect(
        items = state.listings,
        onQueryChanged = { query ->
            actions(Action.List.LoadAround(query ?: state.currentQuery))
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
            Card(
                modifier = Modifier,
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    state = rememberPagerState(pageCount = feedItem.images::size)
                ) { index ->
                    val image = feedItem.images[index]
                    val thumbnail = rememberSharedContent<ImageArgs>(
                        thumbnailSharedElementKey(image.url)
                    ) { args, innerModifier ->
                        AsyncImage(
                            modifier = innerModifier,
                            model = args.url,
                            contentDescription = null,
                            contentScale = args.contentScale
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        thumbnail(
                            ImageArgs(
                                url = image.url,
                                contentScale = ContentScale.Crop
                            ),
                            Modifier
                                .fillMaxSize()
                                .clickable {
                                    actions(
                                        Action.Navigation.Detail(
                                            listingId = feedItem.listing.id,
                                            url = image.url
                                        )
                                    )
                                },
                        )
                        Box {
//                            Text(text = )
                        }
                    }
                }
            }
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
                    text = feedItem.listing.description,
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
    )
}

@Composable
private fun BoxScope.EmptyView(
    status: SyncStatus,
    isEmpty: Boolean
) {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp)
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
