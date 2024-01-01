package com.tunjid.feature.feed

import com.tunjid.data.image.Image
import com.tunjid.data.listing.Listing
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

    sealed class List : Action("List") {
        data class LoadAround(val query: ListingQuery) : List()
        data class GridSize(val numColumns: Int) : List()
    }

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class Detail(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                val route = routeString(
                    path = "listings/$listingId",
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
            val initialIndex: Int,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "listings/${listingId}/gallery/full",
                        queryParams = mapOf(
                            "initialIndex" to listOf(initialIndex.toString()),
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
    val syncStatus: SyncStatus = SyncStatus.Idle,
    @Transient
    val listings: TiledList<ListingQuery, FeedItem> = emptyTiledList()
) : ByteSerializable

val State.isRefreshing get() = syncStatus == SyncStatus.Running

data class FeedItem(
    val listing: Listing,
    val images: List<Image>
)

val FeedItem.id get() = listing.id

