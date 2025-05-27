package com.tunjid.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.tunjid.data.database.dao.FavoriteDao
import com.tunjid.data.favorite.database.model.FavoriteEntity
import com.tunjid.data.database.dao.ListingDao
import com.tunjid.data.database.dao.UserDao
import com.tunjid.data.listing.database.model.ListingEntity
import com.tunjid.data.listing.database.model.UserEntity
import com.tunjid.data.database.dao.MediaDao
import com.tunjid.data.media.database.model.MediaEntity

@Database(
    entities = [
        UserEntity::class,
        MediaEntity::class,
        ListingEntity::class,
        FavoriteEntity::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true,
)

abstract class ListingDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun listingDao(): ListingDao
    abstract fun mediaDao(): MediaDao
    abstract fun userDao(): UserDao
}