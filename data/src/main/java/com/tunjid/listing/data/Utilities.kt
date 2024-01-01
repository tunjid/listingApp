package com.tunjid.listing.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

inline fun <reified In, reified Deferred, reified Out> Flow<List<In>>.withDeferred(
    crossinline deferredFetcher: (In) -> Flow<Deferred>,
    crossinline combiner: (In, Deferred) -> Out
): Flow<List<Out>> =
    flatMapLatest { itemsIn ->
        if (itemsIn.isEmpty()) flowOf(listOf()) else combine(
            flows = itemsIn.map { itemIn ->
                deferredFetch(
                    itemIn = itemIn,
                    deferredFetcher = deferredFetcher,
                    combiner = combiner
                )
            },
            transform = Array<Out>::toList
        )
    }

inline fun <In, Deferred, Out> deferredFetch(
    itemIn: In,
    deferredFetcher: (In) -> Flow<Deferred>,
    crossinline combiner: (In, Deferred) -> Out
): Flow<Out> =
    deferredFetcher(itemIn).map { images ->
        combiner(itemIn, images)
    }