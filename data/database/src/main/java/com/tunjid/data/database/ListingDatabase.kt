package com.tunjid.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tunjid.data.media.database.MediaDao
import com.tunjid.data.media.database.model.MediaEntity
import com.tunjid.data.listing.database.ListingDao
import com.tunjid.data.listing.database.UserDao
import com.tunjid.data.listing.database.model.ListingEntity
import com.tunjid.data.listing.database.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MediaEntity::class,
        ListingEntity::class,
    ],
    version = 1,
    autoMigrations = [],
    exportSchema = true,
)

abstract class ListingDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun mediaDao(): MediaDao
    abstract fun userDao(): UserDao
}