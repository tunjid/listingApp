package com.tunjid.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.data.media.database.model.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    /**
     * Fetches medias matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT * FROM media
            WHERE listing_id = :listingId
            LIMIT :limit
            OFFSET :offset
    """,
    )
    fun mediaFor(
        listingId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<MediaEntity>>

    /**
     * Fetches medias matching the specified query
     */
    @Transaction
    @Query(
        value = """
            SELECT COUNT(*) FROM media
            WHERE listing_id = :listingId
    """,
    )
    fun mediaAvailable(
        listingId: String,
    ): Flow<Long>

    /**
     * Inserts or updates [MediaEntity] in the db under the specified primary keys
     */
    @Upsert
    suspend fun upsertMedia(listings: List<MediaEntity>)
}