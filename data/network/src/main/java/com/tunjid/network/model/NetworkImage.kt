package com.tunjid.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkImage(
    @SerialName("caption")
    val caption: String,
    @SerialName("pictureUrl")
    val pictureUrl: String,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String
)