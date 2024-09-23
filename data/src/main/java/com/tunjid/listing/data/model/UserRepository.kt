package com.tunjid.listing.data.model

import com.tunjid.data.listing.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun user(id: String): Flow<User>
}
