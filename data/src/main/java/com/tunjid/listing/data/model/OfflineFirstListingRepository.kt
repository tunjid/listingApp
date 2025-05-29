package com.tunjid.listing.data.model

import com.tunjid.data.database.dao.ListingDao
import com.tunjid.data.database.dao.MediaDao
import com.tunjid.data.database.dao.UserDao
import com.tunjid.data.database.entities.ListingEntity
import com.tunjid.data.database.entities.MediaEntity
import com.tunjid.data.database.entities.UserEntity
import com.tunjid.data.model.Listing
import com.tunjid.listing.sync.Synchronizer
import com.tunjid.network.ListingNetworkDataSource
import com.tunjid.network.model.NetworkHost
import com.tunjid.network.model.NetworkImage
import com.tunjid.network.model.NetworkListing
import com.tunjid.network.model.price
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstListingRepository @Inject constructor(
    private val listingDao: ListingDao,
    private val mediaDao: MediaDao,
    private val userDao: UserDao,
    private val listingNetworkDataSource: ListingNetworkDataSource,
) : ListingRepository {

    override fun listing(id: String): Flow<Listing> =
        listingDao.listing(id)
            .filterNotNull()
            .distinctUntilChanged()
            .map(ListingEntity::asExternalModel)

    override fun listings(query: ListingQuery): Flow<List<Listing>> =
        listingDao.listings(
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

    override fun listingsAvailable(propertyType: String?): Flow<Long> =
        listingDao.listingsAvailable(propertyType)
            .distinctUntilChanged()

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val networkListings = listingNetworkDataSource.popularListings()
        userDao.upsertUsers(
            networkListings.map { networkListing ->
                networkListing.primaryHost.asUserEntity()
            }
        )
        listingDao.upsertListings(
            networkListings.map { networkListing ->
                networkListing.asEntity(networkListing.primaryHost.id.toString())
            }
        )
        mediaDao.upsertMedia(
            networkListings.flatMap { networkListing ->
                networkListing.medias.mapNotNull { networkImage ->
                    networkImage.asEntity(networkListing.url.toListingId())
                }
            }
        )
        return true
    }
}

private fun NetworkListing.asEntity(hostId: String) = ListingEntity(
    id = url.toListingId(),
    hostId = hostId,
    price = price,
    address = address,
    title = name,
    propertyType = roomType
)

private fun NetworkImage.asEntity(listingId: String) =
    MediaIdRegex.find(thumbnailUrl)?.value?.let { id ->
        MediaEntity(
            id = id,
            listingId = listingId,
            url = pictureUrl.split("?").first(),
            a11yContentDescription = caption
        )
    }

private fun NetworkHost.asUserEntity() = UserEntity(
    id = id.toString(),
    firstName = firstName,
    about = about,
    pictureUrl = pictureUrl,
    memberSince = memberSince,
    isSuperHost = isSuperHost,
)

private fun String.toListingId() = split("/").last()

private val MediaIdRegex =
    Regex("[A-Za-z0-9_]{8}-[A-Za-z0-9_]{4}-[A-Za-z0-9_]{4}-[A-Za-z0-9_]{4}-[A-Za-z0-9_]{12}")


fun ListingEntity.asExternalModel() = Listing(
    id = id,
    hostId = hostId,
    price = price,
    address = address,
    title = title,
    propertyType = propertyType
)