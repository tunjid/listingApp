package com.tunjid.feature.detail

import com.tunjid.data.image.Image
import com.tunjid.data.listing.Listing
import com.tunjid.data.listing.User
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {

    data class LoadImagesAround(
        val query: ImageQuery
    ) : Action("LoadImagesAround")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data object Pop : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class Gallery(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "listings/$listingId/gallery/grid",
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
    val currentQuery: ImageQuery,
    @Transient
    val initialIndex: Int? = null,
    @Transient
    val isInPrimaryNav: Boolean = false,
    @Transient
    val hasSecondaryPanel: Boolean = false,
    @Transient
    val paneAnchor: PaneAnchor? = null,
    @Transient
    val listing: Listing? = null,
    @Transient
    val host: User? = null,
    @Transient
    val listingItems: TiledList<ImageQuery, ListingItem> = emptyTiledList(),
) : ByteSerializable

sealed class ListingItem {
    data class Preview(val url: String) : ListingItem()
    data class Loaded(val image: Image) : ListingItem()
}

val ListingItem.url
    get() = when (this) {
        is ListingItem.Loaded -> image.url
        is ListingItem.Preview -> url
    }