package com.tunjid.data.database.di

import com.tunjid.data.database.ListingDatabase
import com.tunjid.data.favorite.database.FavoriteDao
import com.tunjid.data.media.database.MediaDao
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
    fun providesFavoritesDao(
        listingDatabase: ListingDatabase,
    ): FavoriteDao = listingDatabase.favoriteDao()

    @Provides
    @Singleton
    fun providesListingDao(
        listingDatabase: ListingDatabase,
    ): ListingDao = listingDatabase.listingDao()

    @Provides
    @Singleton
    fun providesImageDao(
        listingDatabase: ListingDatabase,
    ): MediaDao = listingDatabase.mediaDao()

    @Provides
    @Singleton
    fun providesUserDao(
        listingDatabase: ListingDatabase,
    ): UserDao = listingDatabase.userDao()
}