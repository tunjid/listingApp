package com.tunjid.listing.data.model

import com.tunjid.data.listing.Listing
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun isFavorite(listingId: String): Flow<Boolean>

    suspend fun setListingFavorited(listingId: String, isFavorite: Boolean)

    fun favoriteListings(query: ListingQuery): Flow<List<Listing>>

    fun favoritesAvailable(propertyType: String? = null): Flow<Long>
}