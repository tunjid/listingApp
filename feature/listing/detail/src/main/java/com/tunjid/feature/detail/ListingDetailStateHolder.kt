package com.tunjid.feature.detail

import com.tunjid.data.image.Image
import com.tunjid.feature.detail.di.ListingDetailRoute
import com.tunjid.listing.data.model.ImageQuery
import com.tunjid.listing.data.model.ImageRepository
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.UserRepository
import com.tunjid.mutator.ActionStateProducer
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowProducer
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapLatestToMutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.di.restoreState
import com.tunjid.scaffold.globalui.UiState
import com.tunjid.scaffold.globalui.WindowSizeClass
import com.tunjid.scaffold.isInPrimaryNavMutations
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.consumeNavigationActions
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.map
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.MultiStackNav
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

typealias ListingDetailStateHolder = ActionStateProducer<Action, StateFlow<State>>

@AssistedFactory
interface ListingStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: ListingDetailRoute,
    ): ActualEmployeeListStateHolder
}

class ActualEmployeeListStateHolder @AssistedInject constructor(
    listingRepository: ListingRepository,
    imageRepository: ImageRepository,
    userRepository: UserRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    navStateFlow: StateFlow<MultiStackNav>,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: ListingDetailRoute,
) : ListingDetailStateHolder by scope.listingDetailMutator(
    listingRepository = listingRepository,
    imageRepository = imageRepository,
    userRepository = userRepository,
    byteSerializer = byteSerializer,
    uiStateFlow = uiStateFlow,
    navStateFlow = navStateFlow,
    navigationActions = navigationActions,
    savedState = savedState,
    route = route
)

private fun CoroutineScope.listingDetailMutator(
    listingRepository: ListingRepository,
    imageRepository: ImageRepository,
    userRepository: UserRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    navStateFlow: StateFlow<MultiStackNav>,
    navigationActions: (NavigationMutation) -> Unit,
    savedState: ByteArray?,
    route: ListingDetailRoute,
) = actionStateFlowProducer<Action, State>(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = route.initialQuery,
        initialIndex = route.initialIndex,
        listingItems = buildTiledList {
            addAll(
                query = route.initialQuery,
                items = route.startingMediaUrls.map(ListingItem::Preview)
            )
        }
    ),
    mutationFlows = listOf(
        fetchListingMutations(
            listingId = route.listingId,
            listingRepository = listingRepository,
            userRepository = userRepository
        ),
        navStateFlow.isInPrimaryNavMutations(
            route = route,
            mutation = { copy(isInPrimaryNav = it) }
        ),
        uiStateFlow.paneMutations()
    ),
    actionTransform = { actions ->
        actions.toMutationStream(Action::key) {
            when (val action = type()) {
                is Action.Navigation -> action.flow.consumeNavigationActions(
                    navigationActions
                )

                is Action.LoadImagesAround -> action.flow.paginationMutations(
                    imageRepository = imageRepository
                )
            }
        }
    }
)

private fun fetchListingMutations(
    listingId: String,
    listingRepository: ListingRepository,
    userRepository: UserRepository,
): Flow<Mutation<State>> =
    listingRepository.listing(listingId).mapLatestToManyMutations { listing ->
        emit { copy(listing = listing) }
        emitAll(
            userRepository.user(listing.hostId).mapLatestToMutation { host ->
                copy(host = host)
            }
        )
    }

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
            copy(listingItems = images.distinctBy(Image::url).map(ListingItem::Loaded))
        }

private fun StateFlow<UiState>.paneMutations(): Flow<Mutation<State>> =
    map { (it.windowSizeClass > WindowSizeClass.COMPACT) to it.paneAnchor }
        .distinctUntilChanged()
        .mapToMutation { (hasSecondaryPanel, paneAnchor) ->
            copy(
                hasSecondaryPanel = hasSecondaryPanel,
                paneAnchor = paneAnchor,
            )
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