package com.tunjid.feature.listinggallery.pager

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
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.di.restoreState
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
interface PagerGalleryStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: Route,
    ): ActualPagerGalleryStateHolder
}

class ActualPagerGalleryStateHolder @AssistedInject constructor(
    mediaRepository: MediaRepository,
    byteSerializer: ByteSerializer,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: Route,
) : PagerGalleryStateHolder by scope.pagerGalleryMutator(
    mediaRepository = mediaRepository,
    byteSerializer = byteSerializer,
    navigationActions = navigationActions,
    savedState = savedState,
    route = route
)

private fun CoroutineScope.pagerGalleryMutator(
    mediaRepository: MediaRepository,
    byteSerializer: ByteSerializer,
    navigationActions: (NavigationMutation) -> Unit,
    savedState: ByteArray?,
    route: Route,
) = actionStateFlowMutator<Action, State>(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = route.routeParams.initialQuery,
        items = buildTiledList {
            addAll(
                query = route.routeParams.initialQuery,
                items = route.routeParams.startingMediaUrls.mapIndexed(GalleryItem::Preview)
            )
        }
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.LoadImagesAround -> action.flow.paginationMutations(
                    mediaRepository
                )

                is Action.Navigation -> action.flow.consumeNavigationActions(
                    navigationActions
                )
            }
        }
    }
)

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
