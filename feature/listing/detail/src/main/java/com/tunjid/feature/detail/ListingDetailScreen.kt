package com.tunjid.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tunjid.data.listing.Listing
import com.tunjid.data.listing.User
import com.tunjid.listing.feature.listing.detail.R
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.media.Photo
import com.tunjid.scaffold.media.PhotoArgs
import com.tunjid.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun ListingDetailScreen(
    movableSharedElementScope: MovableSharedElementScope,
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = state.listingItems::size
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Box {
            ListingMediaPager(
                movableSharedElementScope = movableSharedElementScope,
                pagerState = pagerState,
                listingId = state.listing?.id,
                listingItems = state.listingItems,
                totalItemCount = state.mediaAvailable,
                actions = actions,
            )
            FilledTonalIconButton(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        vertical = 16.dp,
                        horizontal = 16.dp
                    ),
                onClick = { actions(Action.Navigation.Pop()) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = ""
                )
            }
        }
        ListingInfo(
            listing = state.listing,
            host = state.host,
        )
    }

    // Paginate the header medias
    pagerState.PivotedTilingEffect(
        items = state.listingItems,
        onQueryChanged = { query ->
            actions(Action.LoadImagesAround(query = query ?: state.currentQuery))
        }
    )

    // Close the secondary pane when invoking back since it contains the list view
    SecondaryPaneCloseBackHandler(
        enabled = state.isInPrimaryNav && state.hasSecondaryPanel
    )

    // If the user fully expands the secondary pane, pop this destination back to the feed
    LaunchedEffect(state.hasSecondaryPanel, state.paneAnchor) {
        if (state.hasSecondaryPanel && state.paneAnchor == PaneAnchor.Full) {
            actions(Action.Navigation.Pop())
        }
    }
}

@Composable
private fun ListingMediaPager(
    movableSharedElementScope: MovableSharedElementScope,
    pagerState: PagerState,
    listingId: String?,
    listingItems: List<ListingItem>,
    totalItemCount: Long?,
    actions: (Action) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3)
    ) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            key = { index -> listingItems[index].url }
        ) { index ->
            val item = listingItems[index]
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
                    contentScale = ContentScale.Crop
                ),
                Modifier
                    .fillMaxSize()
                    .clickable {
                        if (listingId != null) actions(
                            Action.Navigation.Gallery(
                                listingId = listingId,
                                url = item.url
                            )
                        )
                    },
            )
        }
        if (listingItems.isNotEmpty() && totalItemCount != null) PageIndicator(
            pagerIndex = listingItems[pagerState.currentPage].index,
            totalItemCount = totalItemCount
        )
    }
}

@Composable
private fun BoxScope.PageIndicator(
    pagerIndex: Int,
    totalItemCount: Long
) {
    Box(
        modifier = Modifier.Companion
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp),
            )
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp,
                ),
            text = stringResource(
                id = R.string.item_count,
                pagerIndex,
                totalItemCount
            )
        )
    }
}

@Composable
private fun ListingInfo(
    listing: Listing?,
    host: User?
) {
    Column {
        ListingInfoHeader(listing)
        HostInfo(host)
    }
}

@Composable
private fun ListingInfoHeader(listing: Listing?) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = listing?.title ?: "",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(
                R.string.property_description,
                listing?.propertyType ?: "",
                listing?.address ?: "",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = listing?.price ?: "")
    }
}

@Composable
private fun HostInfo(host: User?) {
    Surface(
        modifier = Modifier
            .padding(vertical = 16.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(R.string.meet_your_host),
                style = MaterialTheme.typography.headlineSmall,
            )
            Card(
                modifier = Modifier
                    .height(200.dp),
                onClick = { /*TODO*/ }
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HostImage(
                        hostImageUrl = host?.pictureUrl,
                        isSuperHost = host?.isSuperHost == true
                    )
                    HostSummary(
                        firstName = host?.firstName,
                        memberSince = host?.memberSince
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RowScope.HostImage(
    hostImageUrl: String?,
    isSuperHost: Boolean
) {
    Column(
        modifier = Modifier.Companion
            .weight(1f)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            model = hostImageUrl,
            contentDescription = stringResource(R.string.host),
            contentScale = ContentScale.Crop
        )
        if (isSuperHost) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.superhost),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun RowScope.HostSummary(
    firstName: String?,
    memberSince: String?,
) {
    Column(
        modifier = Modifier.Companion
            .weight(1f)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = firstName ?: "")
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = memberSince ?: "")
    }
}
