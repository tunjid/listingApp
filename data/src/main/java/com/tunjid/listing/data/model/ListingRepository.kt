package com.tunjid.listing.data.model

import com.tunjid.data.favorite.database.model.Listing
import com.tunjid.listing.sync.Syncable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ListingQuery(
    val propertyType: String? = null,
    val limit: Long,
    val offset: Long,
)

interface ListingRepository : Syncable {
    fun listing(id: String): Flow<Listing>

    fun listings(query: ListingQuery): Flow<List<Listing>>

    fun listingsAvailable(propertyType: String? = null): Flow<Long>
}
