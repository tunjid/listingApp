package com.tunjid.feature.feed

import com.tunjid.feature.feed.di.isFavorites
import com.tunjid.feature.feed.di.limit
import com.tunjid.feature.feed.di.offset
import com.tunjid.feature.feed.di.propertyType
import com.tunjid.listing.data.model.FavoriteRepository
import com.tunjid.listing.data.model.ListingQuery
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.listing.data.withDeferred
import com.tunjid.listing.sync.SyncManager
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.SuspendingStateHolder
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.mutator.identity
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.di.restoreState
import com.tunjid.scaffold.navigation.NavigationMutation
import com.tunjid.scaffold.navigation.consumeNavigationActions
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.listTiler
import com.tunjid.tiler.queries
import com.tunjid.tiler.toPivotedTileInputs
import com.tunjid.tiler.toTiledList
import com.tunjid.treenav.strings.Route
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

typealias ListingFeedStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
interface ListingFeedStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: Route,
    ): ActualListingFeedStateHolder
}

class ActualListingFeedStateHolder @AssistedInject constructor(
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    favoriteRepository: FavoriteRepository,
    syncManager: SyncManager,
    byteSerializer: ByteSerializer,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: Route,
) : ListingFeedStateHolder by scope.listingFeedStateHolder(
    listingRepository = listingRepository,
    mediaRepository = mediaRepository,
    favoriteRepository = favoriteRepository,
    syncManager = syncManager,
    byteSerializer = byteSerializer,
    navigationActions = navigationActions,
    savedState = savedState,
    route = route
)

fun CoroutineScope.listingFeedStateHolder(
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    favoriteRepository: FavoriteRepository,
    syncManager: SyncManager,
    byteSerializer: ByteSerializer,
    navigationActions: (NavigationMutation) -> Unit,
    savedState: ByteArray?,
    route: Route,
): ListingFeedStateHolder = actionStateFlowMutator(
    initialState = byteSerializer.restoreState(savedState) ?: State(
        currentQuery = ListingQuery(
            propertyType = route.routeParams.propertyType,
            limit = route.routeParams.limit,
            offset = route.routeParams.offset,
        )
    ),
    started = SharingStarted.WhileSubscribed(3000),
    inputs = listOf(
        syncManager.refreshStatusMutations()
    ),
    actionTransform = stateHolder@{ actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.LoadFeed -> action.flow.fetchListingFeedMutations(
                    isFavorites = route.routeParams.isFavorites,
                    listingRepository = listingRepository,
                    mediaRepository = mediaRepository,
                    favoriteRepository = favoriteRepository,
                )

                is Action.Refresh -> action.flow.refreshMutations(
                    syncManager = syncManager
                )

                is Action.SetFavorite -> action.flow.favoriteMutations(
                    favoriteRepository = favoriteRepository
                )

                is Action.Navigation -> action.flow.consumeNavigationActions(
                    navigationActions
                )
            }
        }
    }
)

/**
 * Mutations caused by sync updates
 */
private fun SyncManager.refreshStatusMutations(): Flow<Mutation<State>> =
    status.mapToMutation { copy(syncStatus = it) }

/**
 * On each invocation of the refresh action, check if we're refreshing. If not, request a refresh.
 */
context(SuspendingStateHolder<State>)
private fun Flow<Action.Refresh>.refreshMutations(
    syncManager: SyncManager
): Flow<Mutation<State>> = mapLatestToManyMutations {
    val isRefreshing = state().isRefreshing
    if (!isRefreshing) syncManager.requestSync()

    // Don't change the state, emit itself
    emit { this }
}

private fun Flow<Action.SetFavorite>.favoriteMutations(
    favoriteRepository: FavoriteRepository
): Flow<Mutation<State>> = mapLatest { action ->
    favoriteRepository.setListingFavorited(
        listingId = action.listingId,
        isFavorite = action.isFavorite
    )
    identity()
}

/**
 * Feed mutations as a function of the user's scroll position
 */
context(SuspendingStateHolder<State>)
private suspend fun Flow<Action.LoadFeed>.fetchListingFeedMutations(
    isFavorites: Boolean,
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    favoriteRepository: FavoriteRepository,
): Flow<Mutation<State>> {
    // Read the starting state at the time of subscription
    val startingState = state()

    return scan(
        initial = Pair(
            MutableStateFlow(startingState.currentQuery),
            MutableStateFlow(startingState.numColumns)
        )
    ) { accumulator, action ->
        val (queries, numColumns) = accumulator
        // update backing states as a side effect
        when (action) {
            is Action.LoadFeed.GridSize -> numColumns.value = action.numColumns
            is Action.LoadFeed.LoadAround -> queries.value = action.query
        }
        // Emit the same item with each action
        accumulator
    }
        // Only emit once
        .distinctUntilChanged()
        // Flatmap to the fields defined earlier
        .flatMapLatest { (queries, numColumns) ->
            val listingsAvailable = queries
                .map { it.propertyType }
                .distinctUntilChanged()
                .flatMapLatest { propertyType ->
                    when (isFavorites) {
                        true -> favoriteRepository.favoritesAvailable(propertyType = propertyType)
                        false -> listingRepository.listingsAvailable(propertyType = propertyType)
                    }
                }
            val tileInputs = merge(
                numColumns.map { columns ->
                    Tile.Limiter(
                        maxQueries = 3 * columns,
                        itemSizeHint = null,
                    )
                },
                queries.toPivotedTileInputs(
                    numColumns.map(::listingPivotRequest)
                )
            )
            // Merge all state changes that are a function of loading the list
            merge(
                listingsAvailable.mapToMutation { copy(listingsAvailable = it) },
                queries.mapToMutation { copy(currentQuery = it) },
                numColumns.mapToMutation { copy(numColumns = it) },
                tileInputs.toTiledList(
                    feedItemListTiler(
                        isFavorites = isFavorites,
                        startingQuery = queries.value,
                        listingRepository = listingRepository,
                        mediaRepository = mediaRepository,
                        favoriteRepository = favoriteRepository,
                    )
                )
                    .debounce { fetchedList ->
                        if (fetchedList.isEmpty()) return@debounce 500
                        val isAllLoading = (0 until fetchedList.tileCount)
                            .map { index -> fetchedList[fetchedList.tileAt(index).start] }
                            .all { it is FeedItem.Loading }

                        if (isAllLoading) 500
                        else 0
                    }
                    // The produced list can be debounced to keep the user's scroll
                    // position if the query changes for filtering or sorting reasons.
                    // It can also be introspected and filtered to guarantee the items
                    // produced are always consecutive.
                    // See the project readme for details: https://github.com/tunjid/Tiler
                    .mapToMutation { fetchedList ->
                        // Queries update independently of each other, so duplicates may be emitted.
                        // The maximum amount of items returned is bound by the size of the
                        // view port. Typically << 100 items so the
                        // distinct operation is cheap and fixed.
                        if (!fetchedList.queries().contains(currentQuery)) this
                        else copy(listings = fetchedList.distinctBy(FeedItem::key))
                    }
            )
        }
}

private fun listingPivotRequest(numColumns: Int) =
    PivotRequest<ListingQuery, FeedItem>(
        onCount = numColumns * 3,
        offCount = numColumns * 2,
        comparator = ListingQueryComparator,
        previousQuery = {
            if ((offset - limit) < 0) null
            else copy(offset = offset - limit)
        },
        nextQuery = {
            copy(offset = offset + limit)
        }
    )


private fun feedItemListTiler(
    isFavorites: Boolean,
    startingQuery: ListingQuery,
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    favoriteRepository: FavoriteRepository,
) = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = ListingQueryComparator,
    ),
    fetcher = { query ->
        when (isFavorites) {
            true -> favoriteRepository.favoriteListings(query)
            false -> listingRepository.listings(query)
        }.withDeferred(
            deferredFetcher = { listing ->
                combine(
                    favoriteRepository.isFavorite(listing.id),
                    // This can also be paginated for a fully immersive experience
                    mediaRepository.media(
                        MediaQuery(
                            listingId = listing.id,
                            offset = 0L,
                            limit = 10
                        )
                    ),
                    ::Pair
                )
            },
            combiner = { index, listing, (isFavorite, medias) ->
                val itemIndex = query.offset.toInt() + index
                FeedItem.Loaded(
                    key = "$query-$itemIndex",
                    index = itemIndex,
                    listing = listing,
                    isFavorite = isFavorite,
                    medias = medias
                )
            }
        )
            .onStart<List<FeedItem>> {
                emit(
                    (0 until query.limit.toInt()).map { index ->
                        val itemIndex = query.offset.toInt() + index
                        FeedItem.Loading(
                            key = "$query-$itemIndex",
                            index = itemIndex,
                        )
                    }
                )
            }
    }
)

private val ListingQueryComparator = compareBy(ListingQuery::offset)