package com.tunjid.explore.pager

import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.media.NoOpPlayerManager
import com.tunjid.scaffold.media.PlayerManager
import com.tunjid.scaffold.navigation.NavigationAction
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.editCurrentIfRoute
import com.tunjid.scaffold.navigation.plus
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class Action(val key: String) {

    sealed class Navigation : Action("Navigation"), NavigationAction {
        data class Pop(
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        ) : Navigation()
    }
}

internal suspend fun SuspendingStateHolder<State>.navigationEdits(
    navigationAction: Action.Navigation
) = when (navigationAction) {
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
    @Transient
    val playerManager: PlayerManager = NoOpPlayerManager,
    @Transient
    val items: List<GalleryItem> = emptyList(),
) : ByteSerializable

sealed class GalleryItem {

    data class Preview(
        val url: String,
    ) : GalleryItem()
}

val GalleryItem.url
    get() = when (this) {
        is GalleryItem.Preview -> url
    }
