package com.tunjid.data.image.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.data.image.database.model.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    /**
     * Fetches images matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT * FROM images
            WHERE listing_id = :listingId
            LIMIT :limit
            OFFSET :offset
    """,
    )
    fun mediaFor(
        listingId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<ImageEntity>>

    /**
     * Inserts or updates [ImageEntity] in the db under the specified primary keys
     */
    @Upsert
    suspend fun upsertMedia(listings: List<ImageEntity>)
}