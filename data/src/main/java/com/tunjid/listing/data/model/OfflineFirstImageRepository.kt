package com.tunjid.listing.data.model

import com.tunjid.data.image.Image
import com.tunjid.data.image.database.ImageDao
import com.tunjid.data.image.database.model.ImageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstImageRepository @Inject constructor(
    private val imageDao: ImageDao,
) : ImageRepository {

    override fun images(query: ImageQuery): Flow<List<Image>> =
        imageDao.mediaFor(
            listingId = query.listingId,
            limit = query.limit,
            offset = query.offset
        )
            .distinctUntilChanged()
            .map { entities ->
                entities.map(ImageEntity::asExternalModel)
            }
}

fun ImageEntity.asExternalModel() = Image(
    id = id,
    listingId = listingId,
    url = url,
    a11yContentDescription = a11yContentDescription
)
