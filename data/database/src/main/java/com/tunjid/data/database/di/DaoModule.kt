package com.tunjid.data.database.di

import com.tunjid.data.database.ListingDatabase
import com.tunjid.data.image.database.ImageDao
import com.tunjid.data.listing.database.ListingDao
import com.tunjid.data.listing.database.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    @Singleton
    fun providesListingDao(
        listingDatabase: ListingDatabase,
    ): ListingDao = listingDatabase.listingDao()

    @Provides
    @Singleton
    fun providesImageDao(
        listingDatabase: ListingDatabase,
    ): ImageDao = listingDatabase.imageDao()

    @Provides
    @Singleton
    fun providesUserDao(
        listingDatabase: ListingDatabase,
    ): UserDao = listingDatabase.userDao()
}