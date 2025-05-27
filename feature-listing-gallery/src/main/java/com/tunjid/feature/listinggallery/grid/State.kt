package com.tunjid.feature.listinggallery.grid

import com.tunjid.data.favorite.database.model.Media
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.scaffold.ByteSerializable
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

    sealed class LoadItems : Action("LoadItems") {
        data class GridSize(
            val numOfColumns: Int
        ) : LoadItems()

        data class Around(
            val query: MediaQuery
        ) : LoadItems()
    }

    sealed class Navigation : Action("Navigation"), NavigationAction {

        data class Pop(
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        ) : Navigation()

        data class FullScreen(
            val listingId: String,
            val url: String,
        ) : Navigation() {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/listings/$listingId/gallery/pager",
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
    is Action.Navigation.FullScreen -> navigationAction
    is Action.Navigation.Pop -> {
        val urls = state().items.map { it.url }
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
    val numColumns: Int = 2,
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
        val media: Media,
    ) : GalleryItem()
}

val GalleryItem.url
    get() = when (this) {
        is GalleryItem.Loaded -> media.url
        is GalleryItem.Preview -> url
    }
