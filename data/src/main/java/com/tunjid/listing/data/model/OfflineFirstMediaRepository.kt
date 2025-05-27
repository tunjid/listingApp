package com.tunjid.listing.data.model

import com.tunjid.data.media.Media
import com.tunjid.data.database.dao.MediaDao
import com.tunjid.data.database.entities.MediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstMediaRepository @Inject constructor(
    private val mediaDao: MediaDao,
) : MediaRepository {

    override fun media(query: MediaQuery): Flow<List<Media>> =
        mediaDao.mediaFor(
            listingId = query.listingId,
            limit = query.limit,
            offset = query.offset
        )
            .filterNotNull()
            .distinctUntilChanged()
            .map { entities ->
                entities.map(MediaEntity::asExternalModel)
            }

    override fun mediaAvailable(listingId: String): Flow<Long> =
        mediaDao.mediaAvailable(listingId = listingId)
            .distinctUntilChanged()
}

fun MediaEntity.asExternalModel() = Media(
    id = id,
    listingId = listingId,
    url = url,
    a11yContentDescription = a11yContentDescription
)
