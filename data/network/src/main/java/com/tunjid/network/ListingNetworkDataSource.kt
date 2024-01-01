package com.tunjid.network

import com.tunjid.network.model.NetworkListing

interface ListingNetworkDataSource {
    suspend fun popularListings(): List<NetworkListing>
}