package com.tunjid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Listing(
    val id: String,
    val hostId: String,
    val price: String,
    val address: String,
    val title: String,
    val propertyType: String,
)
