package com.tunjid.data.listing.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.data.listing.database.model.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Transaction
    @Query(
        value = """
            SELECT * FROM listings
            WHERE id == :id
    """,
    )
    fun listing(
        id: String,
    ): Flow<ListingEntity>

    /**
     * Fetches listings matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT * FROM listings
            WHERE 
                CASE WHEN :propertyType
                    IS NULL THEN 1
                    ELSE property_type = :propertyType
                END
            ORDER BY title DESC
            LIMIT :limit
            OFFSET :offset
    """,
    )
    fun listings(
        limit: Long,
        offset: Long,
        propertyType: String? = null,
    ): Flow<List<ListingEntity>>

    @Transaction
    @Query(
        value = """
            SELECT COUNT(*) FROM listings
            WHERE 
                CASE WHEN :propertyType
                    IS NULL THEN 1
                    ELSE property_type = :propertyType
                END
    """,
    )
    fun listingsAvailable(
        propertyType: String? = null,
    ): Flow<Long>

    /**
     * Inserts or updates [listings] in the db under the specified primary keys
     */
    @Upsert
    suspend fun upsertListings(listings: List<ListingEntity>)
}