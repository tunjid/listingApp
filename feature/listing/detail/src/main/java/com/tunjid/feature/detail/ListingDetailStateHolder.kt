package com.tunjid.feature.detail

import com.tunjid.feature.detail.di.initialQuery
import com.tunjid.feature.detail.di.listingId
import com.tunjid.feature.detail.di.startingMediaUrls
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.listing.data.model.UserRepository
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
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
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

typealias ListingDetailStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
interface ListingStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: Route,
    ): ActualListingDetailStateHolder
}

class ActualListingDetailStateHolder @AssistedInject constructor(
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    userRepository: UserRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    navStateFlow: StateFlow<MultiStackNav>,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: Route,
) : ListingDetailStateHolder by scope.listingDetailMutator(
    listingRepository = listingRepository,
    mediaRepository = mediaRepository,
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
    mediaRepository: MediaRepository,
    userRepository: UserRepository,
    byteSerializer: ByteSerializer,
    uiStateFlow: StateFlow<UiState>,
    navStateFlow: StateFlow<MultiStackNav>,
    navigationActions: (NavigationMutation) -> Unit,
    savedState: ByteArray?,
    route: Route,
): ActionStateMutator<Action, StateFlow<State>> = actionStateFlowMutator(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = route.routeParams.initialQuery,
        listingItems = buildTiledList {
            addAll(
                query = route.routeParams.initialQuery,
                items = route.routeParams.startingMediaUrls.mapIndexed(ListingItem::Preview)
            )
        }
    ),
    inputs = listOf(
        mediaRepository.countMutations(
            listingId = route.routeParams.listingId
        ),
        fetchListingMutations(
            listingId = route.routeParams.listingId,
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
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Navigation -> action.flow.consumeNavigationActions(
                    navigationActions
                )

                is Action.LoadImagesAround -> action.flow.paginationMutations(
                    mediaRepository = mediaRepository
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

private fun MediaRepository.countMutations(
    listingId: String
): Flow<Mutation<State>> =
    mediaAvailable(listingId)
        .mapToMutation { copy(mediaAvailable = it) }


context(SuspendingStateHolder<State>)
private suspend fun Flow<Action.LoadImagesAround>.paginationMutations(
    mediaRepository: MediaRepository
): Flow<Mutation<State>> =
    map { it.query }
        .toPivotedTileInputs(mediaPivotRequest())
        .toTiledList(
            mediaRepository.mediaListTiler(
                startingQuery = state().currentQuery
            )
        )
        .mapToMutation { medias ->
            copy(listingItems = medias.distinctBy(ListingItem::url))
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

private fun mediaPivotRequest() = PivotRequest<MediaQuery, ListingItem>(
    onCount = 3,
    offCount = 4,
    comparator = MediaQueryComparator,
    previousQuery = {
        if ((offset - limit) < 0) null
        else copy(offset = offset - limit)
    },
    nextQuery = {
        copy(offset = offset + limit)
    }
)

private fun MediaRepository.mediaListTiler(
    startingQuery: MediaQuery
): ListTiler<MediaQuery, ListingItem> = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = MediaQueryComparator,
    ),
    fetcher = { query ->
        media(query).map { media ->
            media.mapIndexed { index, image ->
                ListingItem.Loaded(
                    index = query.offset.toInt() + index,
                    media = image,
                )
            }
        }
    }
)

private val MediaQueryComparator = compareBy(MediaQuery::offset)