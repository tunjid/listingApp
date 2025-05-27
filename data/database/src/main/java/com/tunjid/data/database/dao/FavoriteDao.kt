package com.tunjid.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.data.favorite.database.model.FavoriteEntity
import com.tunjid.data.listing.database.model.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    /**
     * Fetches medias matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT * FROM favorite
            WHERE listing_id = :listingId
    """,
    )
    fun isFavorite(
        listingId: String,
    ): Flow<FavoriteEntity?>

    /**
     * Inserts or updates [FavoriteEntity] in the db
     */
    @Upsert
    suspend fun setFavorite(favoriteEntity: FavoriteEntity)

    /**
     * Fetches listings matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT * FROM listings
            INNER JOIN favorite
            ON id == listing_id
            WHERE 
                CASE WHEN :propertyType
                    IS NULL THEN 1
                    ELSE property_type = :propertyType
                END
            AND
                isFavorite == true
            ORDER BY title DESC
            LIMIT :limit
            OFFSET :offset
    """,
    )
    fun favoriteListings(
        limit: Long,
        offset: Long,
        propertyType: String? = null,
    ): Flow<List<ListingEntity>>

    /**
     * Fetches listings matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT COUNT(*) FROM listings
            INNER JOIN favorite
            ON id == listing_id
            WHERE 
                CASE WHEN :propertyType
                    IS NULL THEN 1
                    ELSE property_type = :propertyType
                END
            AND
                isFavorite == true
            ORDER BY title DESC
    """,
    )
    fun favoritesAvailable(
        propertyType: String? = null,
    ): Flow<Long>
}