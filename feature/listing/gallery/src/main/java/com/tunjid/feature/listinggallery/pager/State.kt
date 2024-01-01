package com.tunjid.feature.listinggallery.pager

import com.tunjid.data.media.Media
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {
    data class LoadImagesAround(
        val query: MediaQuery
    ) : Action("LoadImagesAround")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data object Pop : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }
    }
}

@Serializable
data class State(
    val currentQuery: MediaQuery,
    @Transient
    val items: TiledList<MediaQuery, GalleryItem> = emptyTiledList(),
) : ByteSerializable

sealed class GalleryItem {

    abstract val index: Int

    data class Preview(
        override val index: Int,
        val url: String,
    ) : GalleryItem()

    data class Loaded(
        override val index: Int,
        val media: Media
    ) : GalleryItem()
}

val GalleryItem.url
    get() = when (this) {
        is GalleryItem.Loaded -> media.url
        is GalleryItem.Preview -> url
    }
