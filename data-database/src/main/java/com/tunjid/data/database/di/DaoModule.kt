package com.tunjid.data.database.di

import com.tunjid.data.database.ListingDatabase
import com.tunjid.data.database.dao.FavoriteDao
import com.tunjid.data.database.dao.ListingDao
import com.tunjid.data.database.dao.MediaDao
import com.tunjid.data.database.dao.UserDao
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