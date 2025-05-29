package com.tunjid.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val about: String,
    val isSuperHost: Boolean,
    val pictureUrl: String,
    val memberSince: String,
)
