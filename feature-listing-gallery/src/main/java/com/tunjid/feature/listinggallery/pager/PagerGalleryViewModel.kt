package com.tunjid.feature.listinggallery.pager

import androidx.lifecycle.ViewModel
import com.tunjid.feature.listinggallery.mediaListTiler
import com.tunjid.feature.listinggallery.mediaPivotRequest
import com.tunjid.feature.listinggallery.pager.di.initialQuery
import com.tunjid.feature.listinggallery.pager.di.startingMediaUrls
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.di.ScreenStateHolderCreator
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.consumeNavigationActions
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

typealias PagerGalleryStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
interface PagerGalleryStateHolderFactory : ScreenStateHolderCreator {
    override fun create(
        scope: CoroutineScope,
        route: Route,
    ): PagerGalleryViewModel
}

class PagerGalleryViewModel @AssistedInject constructor(
    mediaRepository: MediaRepository,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), PagerGalleryStateHolder by scope.mutator(
    mediaRepository = mediaRepository,
    navigationActions = navigationActions,
    route = route
)

private fun CoroutineScope.mutator(
    mediaRepository: MediaRepository,
    navigationActions: (NavigationMutation) -> Unit,
    route: Route,
): PagerGalleryStateHolder = actionStateFlowMutator(
    initialState = State(
        currentQuery = route.routeParams.initialQuery,
        items = route.preSeededNavigationItems()
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.LoadImagesAround -> action.flow.paginationMutations(
                    mediaRepository
                )

                is Action.Navigation -> action.flow
                    .map(::navigationEdits)
                    .consumeNavigationActions(
                        navigationActions
                    )
            }
        }
    }
)

private fun Route.preSeededNavigationItems() = buildTiledList {
    addAll(
        query = routeParams.initialQuery,
        items = routeParams.startingMediaUrls.mapIndexed(GalleryItem::Preview)
    )
}

context(SuspendingStateHolder<State>)
private suspend fun Flow<Action.LoadImagesAround>.paginationMutations(
    mediaRepository: MediaRepository
): Flow<Mutation<State>> =
    map { it.query }
        .toPivotedTileInputs(mediaPivotRequest<GalleryItem>(numColumns = 1))
        .toTiledList(
            mediaRepository.mediaListTiler(
                startingQuery = state().currentQuery,
                mapper = GalleryItem::Loaded
            )
        )
        .mapToMutation { items ->
            copy(items = items.distinctBy(GalleryItem::url))
        }
