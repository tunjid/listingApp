package com.tunjid.feature.listinggallery.grid

import com.tunjid.data.image.Image
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {
    data class LoadImagesAround(
        val query: ImageQuery
    ) : Action("LoadImagesAround")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class FullScreen(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "listings/$listingId/gallery/pager",
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
    val items: TiledList<ImageQuery, GalleryItem> = emptyTiledList(),
) : ByteSerializable

sealed class GalleryItem {
    data class Preview(val url: String) : GalleryItem()
    data class Loaded(val image: Image) : GalleryItem()
}

val GalleryItem.url
    get() = when (this) {
        is GalleryItem.Loaded -> image.url
        is GalleryItem.Preview -> url
    }
