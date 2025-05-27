package com.tunjid.listing.data.model

import com.tunjid.data.listing.User
import com.tunjid.data.database.dao.UserDao
import com.tunjid.data.listing.database.model.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstUserRepository @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {
    override fun user(id: String): Flow<User> =
        userDao.user(id)
            .filterNotNull()
            .distinctUntilChanged()
            .map(UserEntity::asExternalModel)
}

fun UserEntity.asExternalModel() = User(
    id = id,
    firstName = firstName,
    about = about,
    pictureUrl = pictureUrl,
    memberSince = memberSince,
    isSuperHost = isSuperHost,
)
