package com.tunjid.feature.listinggallery.pager

import com.tunjid.data.image.Image
import com.tunjid.feature.listinggallery.pager.di.PagerGalleryRoute
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.listing.data.model.ImageRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.di.restoreState
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.listTiler
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
    imageRepository: ImageRepository,
    byteSerializer: ByteSerializer,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: PagerGalleryRoute,
) : PagerGalleryStateHolder by scope.listingDetailMutator(
    imageRepository = imageRepository,
    byteSerializer = byteSerializer,
    savedState = savedState,
    route = route
)

private fun CoroutineScope.listingDetailMutator(
    imageRepository: ImageRepository,
    byteSerializer: ByteSerializer,
    savedState: ByteArray?,
    route: PagerGalleryRoute,
) = actionStateFlowProducer<Action, State>(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = route.initialQuery,
        items = buildTiledList {
            addAll(
                query = route.initialQuery,
                items = route.startingMediaUrls.map(GalleryItem::Preview)
            )
        }
    ),
    actionTransform = { actions ->
        actions.toMutationStream(Action::key) {
            when (val action = type()) {
                is Action.LoadImagesAround -> action.flow.paginationMutations(imageRepository)
            }
        }
    }
)

context(SuspendingStateHolder<State>)
private suspend fun Flow<Action.LoadImagesAround>.paginationMutations(
    imageRepository: ImageRepository
): Flow<Mutation<State>> =
    map { it.query }
        .toPivotedTileInputs(imagesPivotRequest())
        .toTiledList(
            imageRepository.imageListTiler(
                startingQuery = state().currentQuery
            )
        )
        .mapToMutation { images ->
            images.distinctBy { }
            copy(items = images.distinctBy(Image::url).map(GalleryItem::Loaded))
        }


private fun imagesPivotRequest() = PivotRequest<ImageQuery, Image>(
    onCount = 3,
    offCount = 4,
    comparator = ImageQueryComparator,
    previousQuery = {
        if ((offset - limit) < 0) null
        else copy(offset = offset - limit)
    },
    nextQuery = {
        copy(offset = offset + limit)
    }
)

private fun ImageRepository.imageListTiler(
    startingQuery: ImageQuery
) = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = ImageQueryComparator,
    ),
    fetcher = ::images
)

private val ImageQueryComparator = compareBy(ImageQuery::offset)