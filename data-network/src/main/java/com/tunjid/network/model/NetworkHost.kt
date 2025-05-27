package com.tunjid.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkHost(
    @SerialName("id")
    val id: Long,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("about")
    val about: String,
    @SerialName("memberSince")
    val memberSince: String,
    @SerialName("pictureUrl")
    val pictureUrl: String,
    @SerialName("isSuperHost")
    val isSuperHost: Boolean,
)