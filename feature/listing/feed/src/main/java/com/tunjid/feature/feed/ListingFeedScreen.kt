package com.tunjid.feature.feed

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.composables.scrollbars.scrollable.grid.scrollbarState
import com.tunjid.listing.feature.listing.feed.R
import com.tunjid.listing.sync.SyncStatus
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.media.Photo
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.treenav.adaptive.moveablesharedelement.movableSharedElementOf
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.ui.FastScrollbar
import kotlinx.coroutines.flow.first

@Composable
fun ListingFeedScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val updatedItems by rememberUpdatedState(state.listings)
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
                    key = FeedItem::key,
                    span = {
                        actions(Action.LoadFeed.GridSize(maxLineSpan))
                        GridItemSpan(currentLineSpan = 1)
                    },
                    itemContent = { feedItem ->
                        FeedItemCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(fadeInSpec = null),
                            feedItem = feedItem,
                            actions = actions,
                        )
                    }
                )
            }
        }
        val scrollbarState = gridState.scrollbarState(
            itemsAvailable = state.listingsAvailable.toInt(),
            itemIndex = { itemInfo ->
                updatedItems.getOrNull(itemInfo.index)?.index ?: -1
            }
        )

        FastScrollbar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 56.dp)
                .align(Alignment.BottomEnd)
                .width(12.dp),
            state = scrollbarState,
            scrollInProgress = gridState.isScrollInProgress,
            orientation = Orientation.Vertical,
            onThumbMoved = gridState.rememberScrollbarThumbMover(
                state = state,
                actions = actions
            ),
        )
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
            val pagerState = rememberPagerState(pageCount = feedItem::pagerSize)
            Card(
                modifier = Modifier,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .run {
                            if (feedItem is FeedItem.Loading) background(shimmerBrush())
                            else this
                        },
                ) {
                    FeedMediaPager(
                        pagerState = pagerState,
                        feedItem = feedItem,
                        actions = actions
                    )
                    if (feedItem is FeedItem.Loaded && feedItem.media.isNotEmpty()) MaximizePager(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        listingId = feedItem.listing.id,
                        url = feedItem.media[pagerState.currentPage].url,
                        actions = actions,
                    )
                    if (feedItem is FeedItem.Loaded) FavoriteButton(
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
    if (feedItem is FeedItem.Loaded || feedItem is FeedItem.Preview) HorizontalPager(
        state = pagerState,
        key = { index -> feedItem.media[index].url }
    ) { index ->
        val media = feedItem.media[index]
        val thumbnail = movableSharedElementOf<PhotoArgs>(
            thumbnailSharedElementKey(media.url)
        ) { args, innerModifier ->
            Photo(
                modifier = innerModifier,
                args = args
            )
        }
        thumbnail(
            PhotoArgs(
                url = media.url,
                contentScale = ContentScale.Crop
            ),
            Modifier
                .fillMaxSize()
                .clickable {
                    if (feedItem is FeedItem.Loaded) actions(
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
            text = when (feedItem) {
                is FeedItem.Loaded -> feedItem.listing.title
                is FeedItem.Loading -> ""
                is FeedItem.Preview -> ""
            },
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            text = when (feedItem) {
                is FeedItem.Loaded -> feedItem.listing.address
                is FeedItem.Loading -> ""
                is FeedItem.Preview -> ""
            },
            fontSize = 12.sp
        )
        Text(
            text = when (feedItem) {
                is FeedItem.Loaded -> feedItem.listing.price
                is FeedItem.Loading -> ""
                is FeedItem.Preview -> ""
            },
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

@Composable
private fun LazyGridState.rememberScrollbarThumbMover(
    state: State,
    actions: (Action) -> Unit,
): (Float) -> Unit {
    val updatedListings by rememberUpdatedState(state.listings)
    return com.tunjid.composables.scrollbars.scrollable.rememberScrollbarThumbMover(
        itemsAvailable = state.listingsAvailable.toInt()
    ) mover@{ indexToFind ->
        // Trigger the load to fetch the data required
        actions(
            Action.LoadFeed.LoadAround(
                state.currentQuery.scrollTo(index = indexToFind)
            )
        )

        // Fast path
        val fastIndex = updatedListings.indexOfFirst { it.index == indexToFind }
            .takeIf { it > -1 }
        if (fastIndex != null) return@mover scrollToItem(fastIndex)

        // Slow path
        scrollToItem(
            snapshotFlow { updatedListings.indexOfFirst { it.index == indexToFind } }
                .first { it > -1 }
        )
    }
}

@Composable
fun shimmerBrush(
    targetValue: Float = 1000f
): Brush {
    val shimmerColors = remember {
        listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )
    }

    val transition = rememberInfiniteTransition(
        label = "Shimmer transition"
    )
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        label = "Shimmer animation",
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        )
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(
            x = translateAnimation.value,
            y = translateAnimation.value
        )
    )
}