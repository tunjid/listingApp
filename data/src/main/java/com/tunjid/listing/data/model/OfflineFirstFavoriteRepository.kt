package com.tunjid.listing.data.model

import com.tunjid.data.database.dao.FavoriteDao
import com.tunjid.data.database.entities.FavoriteEntity
import com.tunjid.data.database.entities.ListingEntity
import com.tunjid.data.model.Listing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstFavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoriteRepository {


    override fun isFavorite(listingId: String): Flow<Boolean> =
        favoriteDao.isFavorite(
            listingId = listingId,
        )
            .distinctUntilChanged()
            .map { nullableFavoriteEntity ->
                nullableFavoriteEntity?.isFavorite ?: false
            }

    override suspend fun setListingFavorited(listingId: String, isFavorite: Boolean) {
        favoriteDao.setFavorite(
            FavoriteEntity(
                listingId = listingId,
                isFavorite = isFavorite
            )
        )
    }

    override fun favoriteListings(query: ListingQuery): Flow<List<Listing>> =
        favoriteDao.favoriteListings(
            limit = query.limit,
            offset = query.offset,
            propertyType = query.propertyType
        )
            // Room emits for any database invalidation;
            // only emit is the query range has changed
            .distinctUntilChanged()
            .map { listingEntities ->
                listingEntities.map(ListingEntity::asExternalModel)
            }

    override fun favoritesAvailable(propertyType: String?): Flow<Long> =
        favoriteDao.favoritesAvailable(propertyType)
            .distinctUntilChanged()
}

