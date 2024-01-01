package com.tunjid.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tunjid.data.image.database.ImageDao
import com.tunjid.data.image.database.model.ImageEntity
import com.tunjid.data.listing.database.ListingDao
import com.tunjid.data.listing.database.UserDao
import com.tunjid.data.listing.database.model.ListingEntity
import com.tunjid.data.listing.database.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ImageEntity::class,
        ListingEntity::class,
    ],
    version = 1,
    autoMigrations = [],
    exportSchema = true,
)

abstract class ListingDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun imageDao(): ImageDao
    abstract fun userDao(): UserDao
}