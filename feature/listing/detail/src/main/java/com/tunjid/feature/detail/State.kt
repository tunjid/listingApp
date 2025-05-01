package com.tunjid.feature.detail

import com.tunjid.data.listing.Listing
import com.tunjid.data.listing.User
import com.tunjid.data.media.Media
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.globalui.PaneAnchor
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.editCurrentIfRoute
import com.tunjid.scaffold.navigation.plus
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.routeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {

    data class LoadImagesAround(
        val query: MediaQuery
    ) : Action("LoadImagesAround")

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class Pop(
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        ) : Navigation()

        data class Gallery(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/listings/$listingId/gallery/grid",
                        queryParams = mapOf(
                            "url" to listOf(url)
                        )
                    ).toRoute
                )
            }
        }
    }
}

internal suspend fun SuspendingStateHolder<State>.navigationEdits(
    navigationAction: Action.Navigation
) = when (navigationAction) {
    is Action.Navigation.Gallery -> navigationAction

    is Action.Navigation.Pop -> {
        val urls = state().listingItems.map { it.url }
        navigationAction.copy(
            navigationMutation = navigationAction.navigationMutation + {
                editCurrentIfRoute("url" to urls)
            }
        )
    }
}

@Serializable
data class State(
    val currentQuery: MediaQuery,
    @Transient
    val mediaAvailable: Long? = null,
    @Transient
    val paneAnchor: PaneAnchor? = null,
    @Transient
    val listing: Listing? = null,
    @Transient
    val host: User? = null,
    @Transient
    val listingItems: TiledList<MediaQuery, ListingItem> = emptyTiledList(),
) : ByteSerializable

sealed class ListingItem {

    abstract val index: Int

    data class Preview(
        override val index: Int,
        val url: String
    ) : ListingItem()

    data class Loaded(
        override val index: Int,
        val media: Media
    ) : ListingItem()
}

val ListingItem.url
    get() = when (this) {
        is ListingItem.Loaded -> media.url
        is ListingItem.Preview -> url
    }