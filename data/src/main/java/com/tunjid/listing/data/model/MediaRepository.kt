package com.tunjid.listing.data.model

import com.tunjid.data.media.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class MediaQuery(
    val listingId: String,
    val limit: Long,
    val offset: Long,
)

interface MediaRepository {
    fun media(query: MediaQuery): Flow<List<Media>>

    fun mediaAvailable(
        listingId: String,
    ): Flow<Long>
}