package com.tunjid.data.listing.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.data.listing.database.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Transaction
    @Query(
        value = """
            SELECT * FROM users
            WHERE id = :id
    """,
    )
    fun user(id: String): Flow<UserEntity>

    @Upsert
    suspend fun upsertUsers(listings: List<UserEntity>)
}