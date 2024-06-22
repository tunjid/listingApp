package com.tunjid.feature.feed

import androidx.lifecycle.ViewModel
import com.tunjid.feature.feed.di.initialQuery
import com.tunjid.feature.feed.di.isFavorites
import com.tunjid.feature.feed.di.startingMediaUrls
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
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    ): ListingFeedViewModel
}

class ListingFeedViewModel @AssistedInject constructor(
    listingRepository: ListingRepository,
    mediaRepository: MediaRepository,
    favoriteRepository: FavoriteRepository,
    syncManager: SyncManager,
    byteSerializer: ByteSerializer,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), ActionStateMutator<Action, StateFlow<State>> by scope.listingFeedStateHolder(
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
    initialState = byteSerializer.restoreState<State>(savedState)
        ?.copy(listings = route.preSeededNavigationItems())
        ?: State(
            currentQuery = route.routeParams.initialQuery,
            listings = route.preSeededNavigationItems(),
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

private fun Route.preSeededNavigationItems() = buildTiledList<ListingQuery, FeedItem> {
    this.addAll(
        query = routeParams.initialQuery,
        items = listOf(
            FeedItem.Preview(
                index = 0,
                key = "${routeParams.initialQuery}-0",
                media = routeParams.startingMediaUrls.map(::FeedMedia)
            )
        )
    )
}

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
                    .mapToMutation { fetchedList ->
                        // Queries update independently of each other, so duplicates may be emitted.
                        // The maximum amount of items returned is bound by the size of the
                        // view port. Typically << 100 items so the
                        // distinct operation is cheap and fixed.
                        if (!fetchedList.queries().contains(currentQuery)) this
                        else copy(
                            listings = filterPlaceholdersFrom(fetchedList)
                                .distinctBy(FeedItem::key)
                        )
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
            combiner = { index, listing, (isFavorite, media) ->
                val itemIndex = query.offset.toInt() + index
                FeedItem.Loaded(
                    key = "$query-$itemIndex",
                    index = itemIndex,
                    listing = listing,
                    isFavorite = isFavorite,
                    media = media.map { FeedMedia(it.url) }
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
                // Add a delay so the shimmer effect is visible to simulate async fetch
                delay(1500)
            }
    }
)

/**
 * When returning from the backstack, the paging pipeline will be started
 * again, causing placeholders to be emitted.
 *
 * To keep preserve the existing state from being overwritten by
 * placeholders, the following algorithm iterates over each tile (chunk) of queries in the
 * [TiledList] to see if placeholders are displacing loaded items.
 *
 * If a displacement were to occur, favor the existing items over the displacing placeholders.
 *
 * Algorithm is O(2 * (3*NumOfColumns)).
 * See the project readme for details: https://github.com/tunjid/Tiler
 */
private fun State.filterPlaceholdersFrom(
    fetchedList: TiledList<ListingQuery, FeedItem>
) = buildTiledList {
    val existingMap = 0.until(listings.tileCount).associateBy(
        keySelector = listings::queryAtTile,
        valueTransform = { tileIndex ->
            val existingTile = listings.tileAt(tileIndex)
            listings.subList(
                fromIndex = existingTile.start,
                toIndex = existingTile.end
            )
        }
    )
    for (tileIndex in 0 until fetchedList.tileCount) {
        val fetchedTile = fetchedList.tileAt(tileIndex)
        val fetchedQuery = fetchedList.queryAtTile(tileIndex)
        when (fetchedList[fetchedTile.start]) {
            // Items are already loaded, no swap necessary
            is FeedItem.Loaded -> addAll(
                query = fetchedQuery,
                items = fetchedList.subList(
                    fromIndex = fetchedTile.start,
                    toIndex = fetchedTile.end,
                )
            )
            // Placeholder chunk in fetched list, check if loaded items are in the previous list
            is FeedItem.Preview,
            is FeedItem.Loading -> when (val existingChunk = existingMap[fetchedQuery]) {
                // No existing items, reuse placeholders
                null -> addAll(
                    query = fetchedQuery,
                    items = fetchedList.subList(
                        fromIndex = fetchedTile.start,
                        toIndex = fetchedTile.end,
                    )
                )

                // Reuse existing items
                else -> addAll(
                    query = fetchedQuery,
                    items = existingChunk
                )
            }
        }
    }
}

private val ListingQueryComparator = compareBy(ListingQuery::offset)
