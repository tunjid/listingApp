package com.tunjid.network.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkListing(
    @SerialName("url")
    val url: String,
    @SerialName("pricing")
    val pricing: NetworkPricing,
    @SerialName("primaryHost")
    val primaryHost: NetworkHost,
    @SerialName("address")
    val description: String,
    @SerialName("name")
    val name: String,
    @SerialName("roomType")
    val roomType: String,
    @SerialName("photos")
    val images: List<NetworkImage> // the 1st image is the cover image for the listing
)

val NetworkListing.price get() = pricing.rate.amount.toString()

@Serializable
data class NetworkPricing(
    val rate: NetworkRate,
)

@Serializable
data class NetworkRate(
    val amount: Int,
)

