package com.tunjid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val id: String,
    val listingId: String,
    val url: String,
    val a11yContentDescription: String
)