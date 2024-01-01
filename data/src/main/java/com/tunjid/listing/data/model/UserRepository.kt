package com.tunjid.listing.data.model

import com.tunjid.data.listing.Listing
import com.tunjid.data.listing.User
import com.tunjid.listing.sync.Syncable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface UserRepository {
    fun user(id: String): Flow<User>
}
