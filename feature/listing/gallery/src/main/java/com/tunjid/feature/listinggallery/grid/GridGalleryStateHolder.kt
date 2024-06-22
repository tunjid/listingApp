package com.tunjid.feature.listinggallery.grid

import androidx.lifecycle.ViewModel
import com.tunjid.feature.listinggallery.grid.di.initialQuery
import com.tunjid.feature.listinggallery.grid.di.startingMediaUrls
import com.tunjid.feature.listinggallery.mediaListTiler
import com.tunjid.feature.listinggallery.mediaPivotRequest
import com.tunjid.listing.data.model.MediaQuery
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
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

typealias GridGalleryStateHolder = ActionStateMutator<Action, StateFlow<State>>

@AssistedFactory
interface GridGalleryStateHolderFactory {
    fun create(
        scope: CoroutineScope,
        savedState: ByteArray?,
        route: Route,
    ): GridGalleryViewModel
}

class GridGalleryViewModel @AssistedInject constructor(
    mediaRepository: MediaRepository,
    byteSerializer: ByteSerializer,
    navigationActions: (@JvmSuppressWildcards NavigationMutation) -> Unit,
    @Assisted scope: CoroutineScope,
    @Assisted savedState: ByteArray?,
    @Assisted route: Route,
) : ViewModel(
    viewModelScope = scope,
), GridGalleryStateHolder by scope.gridGalleryMutator(
    mediaRepository = mediaRepository,
    navigationActions = navigationActions,
    byteSerializer = byteSerializer,
    savedState = savedState,
    route = route
)

private fun CoroutineScope.gridGalleryMutator(
    mediaRepository: MediaRepository,
    navigationActions: (NavigationMutation) -> Unit,
    byteSerializer: ByteSerializer,
    savedState: ByteArray?,
    route: Route,
) = actionStateFlowMutator<Action, State>(
    initialState = byteSerializer.restoreState<State>(savedState)
        ?.copy(items = route.preSeededNavigationItems())
        ?: State(
            currentQuery = route.routeParams.initialQuery,
            items = route.preSeededNavigationItems()
        ),
    actionTransform = { actions ->
        actions.toMutationStream(keySelector = Action::key) {
            when (val action = type()) {
                is Action.LoadItems -> action.flow.loadMutations(
                    mediaRepository = mediaRepository
                )

                is Action.Navigation -> action.flow
                    .map(::navigationEdits)
                    .consumeNavigationActions(navigationActions)
            }
        }
    }
)

private fun Route.preSeededNavigationItems() = buildTiledList {
    this.addAll(
        query = routeParams.initialQuery,
        items = routeParams.startingMediaUrls.mapIndexed(GalleryItem::Preview)
    )
}

context(SuspendingStateHolder<State>)
private suspend fun Flow<Action.LoadItems>.loadMutations(
    mediaRepository: MediaRepository
): Flow<Mutation<State>> {
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
            is Action.LoadItems.GridSize -> numColumns.value = action.numOfColumns
            is Action.LoadItems.Around -> queries.value = action.query
        }
        // Emit the same item with each action
        accumulator
    }
        // Only emit once
        .distinctUntilChanged()
        // Flatmap to the fields defined earlier
        .flatMapLatest { (queries, numColumns) ->
            val tileInputs = merge(
                numColumns.map { columns ->
                    Tile.Limiter(
                        maxQueries = 3 * columns,
                        itemSizeHint = null,
                    )
                },
                queries.toPivotedTileInputs(
                    numColumns.map<Int, PivotRequest<MediaQuery, GalleryItem>>(::mediaPivotRequest)
                )
            )
            // Merge all state changes that are a function of loading the list
            merge(
                queries.mapToMutation { copy(currentQuery = it) },
                numColumns.mapToMutation { copy(numColumns = it) },
                tileInputs.toTiledList(
                    mediaRepository.mediaListTiler(
                        startingQuery = state().currentQuery,
                        mapper = GalleryItem::Loaded
                    )
                )
                    .mapToMutation { items ->
                        copy(items = items.distinctBy(GalleryItem::url))
                    }
            )
        }
}
