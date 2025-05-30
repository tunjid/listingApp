package com.tunjid.feature.detail

import androidx.lifecycle.ViewModel
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
import com.tunjid.scaffold.di.AssistedViewModelFactory
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
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

@AssistedFactory
interface ListingViewModelFactory : AssistedViewModelFactory {
    override fun create(
        scope: CoroutineScope,
        route: Route,
    ): ListingDetailViewModel
}

class ListingDetailViewModel @AssistedInject constructor(
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    userRepository: UserRepository,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), ActionStateMutator<Action, StateFlow<State>> by scope.actionStateFlowMutator(
    initialState = State(
        currentQuery = route.initialQuery,
        listingItems = route.preSeededNavigationItems()
    ),
    inputs = listOf(
        mediaRepository.countMutations(
            listingId = route.listingId
        ),
        fetchListingMutations(
            listingId = route.listingId,
            listingRepository = listingRepository,
            userRepository = userRepository
        ),
    ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.Navigation -> action.flow
                    .map(::navigationEdits)
                    .consumeNavigationActions(
                        navigationActions
                    )

                is Action.LoadImagesAround -> action.flow.paginationMutations(
                    mediaRepository = mediaRepository
                )
            }
        }
    }
)

private fun Route.preSeededNavigationItems() = buildTiledList {
    addAll(
        query = initialQuery,
        items = startingMediaUrls.mapIndexed(ListingItem::Preview)
    )
}

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