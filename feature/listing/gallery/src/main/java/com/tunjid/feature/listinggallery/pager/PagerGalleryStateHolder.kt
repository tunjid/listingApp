package com.tunjid.feature.listinggallery.pager

import com.tunjid.data.media.Media
import com.tunjid.feature.listinggallery.mediaListTiler
import com.tunjid.feature.listinggallery.mediaPivotRequest
import com.tunjid.feature.listinggallery.pager.di.PagerGalleryRoute
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.di.restoreState
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.consumeNavigationActions
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.map
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

typealias PagerGalleryStateHolder = ActionStateProducer<Action, StateFlow<State>>

@AssistedFactory
interface PagerGalleryStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: PagerGalleryRoute,
    ): ActualPagerGalleryStateHolder
}

class ActualPagerGalleryStateHolder @AssistedInject constructor(
    mediaRepository: MediaRepository,
    byteSerializer: ByteSerializer,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: PagerGalleryRoute,
) : PagerGalleryStateHolder by scope.listingDetailMutator(
    mediaRepository = mediaRepository,
    byteSerializer = byteSerializer,
    navigationActions = navigationActions,
    savedState = savedState,
    route = route
)

private fun CoroutineScope.listingDetailMutator(
    mediaRepository: MediaRepository,
    byteSerializer: ByteSerializer,
    navigationActions: (NavigationMutation) -> Unit,
    savedState: ByteArray?,
    route: PagerGalleryRoute,
) = actionStateFlowProducer<Action, State>(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = route.initialQuery,
        items = buildTiledList {
            addAll(
                query = route.initialQuery,
                items = route.startingMediaUrls.mapIndexed(GalleryItem::Preview)
            )
        }
    ),
    actionTransform = { actions ->
        actions.toMutationStream(Action::key) {
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
