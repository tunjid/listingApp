package com.tunjid.feature.feed

import com.tunjid.data.listing.Listing
import com.tunjid.data.media.Media
import com.tunjid.feature.feed.di.ListingFeedRoute
import com.tunjid.listing.data.model.ListingQuery
import com.tunjid.listing.sync.SyncStatus
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.current
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import com.tunjid.treenav.swap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber


sealed class Action(val key: String) {

    data object Refresh : Action("Refresh")

    data class SetFavorite(
        val listingId: String,
        val isFavorite: Boolean,
    ) : Action("SetFavorite")

    sealed class LoadFeed : Action("List") {
        data class LoadAround(val query: ListingQuery) : LoadFeed()
        data class GridSize(val numColumns: Int) : LoadFeed()
    }

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class Detail(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                val route = routeString(
                    path = "/listings/$listingId",
                    queryParams = mapOf(
                        "url" to listOf(url)
                    )
                ).toRoute

                // Swap the top route if we're in the secondary panel
                when (navState.current) {
                    is ListingFeedRoute -> navState.push(route)
                    else -> navState.swap(route)
                }
            }
        }

        data class Gallery(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/listings/${listingId}/gallery/pager",
                        queryParams = mapOf(
                            "url" to listOf(url)
                        )
                    ).toRoute
                )
            }
        }
    }
}

@Serializable
data class State(
    @ProtoNumber(1)
    val currentQuery: ListingQuery,
    @Transient
    val numColumns: Int = 1,
    @Transient
    val listingsAvailable: Long = 0L,
    @Transient
    val syncStatus: SyncStatus = SyncStatus.Idle,
    @Transient
    val listings: TiledList<ListingQuery, FeedItem> = emptyTiledList()
) : ByteSerializable

val State.isRefreshing get() = syncStatus == SyncStatus.Running

sealed class FeedItem {
    abstract val key: String
    abstract val index: Int

    data class Loading(
        override val key: String,
        override val index: Int,
    ) : FeedItem()

    data class Loaded(
        override val key: String,
        override val index: Int,
        val listing: Listing,
        val isFavorite: Boolean,
        val medias: List<Media>
    ) : FeedItem()
}

val FeedItem.pagerSize: Int
    get() = when (this) {
        is FeedItem.Loaded -> medias.size
        is FeedItem.Loading -> 0
    }