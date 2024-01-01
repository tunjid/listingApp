package com.tunjid.data.image

import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val id: String,
    val listingId: String,
    val url: String,
    val a11yContentDescription: String
)