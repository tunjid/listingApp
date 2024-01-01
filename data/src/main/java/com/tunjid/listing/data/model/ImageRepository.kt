package com.tunjid.listing.data.model

import com.tunjid.data.image.Image
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ImageQuery(
    val listingId: String,
    val limit: Long,
    val offset: Long,
)

interface ImageRepository {
    fun images(query: ImageQuery): Flow<List<Image>>
}