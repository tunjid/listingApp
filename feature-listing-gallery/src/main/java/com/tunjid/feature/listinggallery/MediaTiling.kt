package com.tunjid.feature.listinggallery

import com.tunjid.data.model.Media
import com.tunjid.listing.data.model.MediaQuery
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.tiler.ListTiler
import com.tunjid.tiler.PivotRequest
import com.tunjid.tiler.Tile
import com.tunjid.tiler.listTiler
import kotlinx.coroutines.flow.map

fun <T> mediaPivotRequest(
    numColumns: Int
) = PivotRequest<MediaQuery, T>(
    onCount = numColumns * 3,
    offCount = numColumns * 4,
    comparator = MediaQueryComparator,
    previousQuery = {
        if (offset - limit < 0) null
        else copy(offset = offset - limit)
    },
    nextQuery = {
        copy(offset = offset + limit)
    }
)

fun <T> MediaRepository.mediaListTiler(
    startingQuery: MediaQuery,
    mapper: (Int, Media) -> T,
): ListTiler<MediaQuery, T> = listTiler(
    order = Tile.Order.PivotSorted(
        query = startingQuery,
        comparator = MediaQueryComparator,
    ),
    fetcher = { query ->
        media(query).map { media ->
            media.mapIndexed { index, image ->
                mapper(
                    query.offset.toInt() + index,
                    image,
                )
            }
        }
    }
)

private val MediaQueryComparator = compareBy(MediaQuery::offset)