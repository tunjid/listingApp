package com.tunjid.feature.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tunjid.data.listing.Listing
import com.tunjid.scaffold.ImageArgs
import com.tunjid.scaffold.adaptive.rememberSharedContent
import com.tunjid.scaffold.adaptive.thumbnailSharedElementKey
import com.tunjid.scaffold.globalui.InsetFlags
import com.tunjid.scaffold.globalui.NavVisibility
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.globalui.ScreenUiState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.scaffold.SecondaryPaneCloseBackHandler
import com.tunjid.tiler.compose.PivotedTilingEffect

@Composable
fun ListingDetailScreen(
    modifier: Modifier = Modifier,
    state: State,
    actions: (Action) -> Unit
) {
    ScreenUiState(
        UiState(
            toolbarTitle = state.listing?.title ?: "",
            toolbarOverlaps = true,
            toolbarShows = false,
            fabShows = false,
            navVisibility = NavVisibility.Gone,
            insetFlags = InsetFlags.NONE,
            statusBarColor = Color.Black.copy(alpha = 0.4f).toArgb()
        )
    )

    val pagerState = rememberPagerState(
        pageCount = state.listingItems::size
    )

    Column(
        modifier = modifier,
    ) {
        ListingMediaPager(
            pagerState = pagerState,
            listingId = state.listing?.id,
            listingItems = state.listingItems,
            actions = actions,
        )
        ListingInfo(listing = state.listing)
    }

    // Paginate the header images
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
            actions(Action.Navigation.Pop)
        }
    }
}

@Composable
private fun ListingMediaPager(
    pagerState: PagerState,
    listingId: String?,
    listingItems: List<ListingItem>,
    actions: (Action) -> Unit,
) {
    HorizontalPager(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9),
        state = pagerState
    ) { index ->
        val item = listingItems[index]
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
}

@Composable
private fun ListingInfo(listing: Listing?) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(text = listing?.propertyType ?: "")
        Text(text = listing?.description ?: "")
        Text(text = listing?.price ?: "")
        Text(text = listing?.description ?: "")
        Button(
            modifier = Modifier.wrapContentSize(),
            onClick = { /*TODO*/ }
        ) {
        }
    }
}
