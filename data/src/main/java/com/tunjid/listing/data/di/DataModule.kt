package com.tunjid.listing.data.di

import com.tunjid.listing.data.model.FavoriteRepository
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.listing.data.model.OfflineFirstFavoriteRepository
import com.tunjid.listing.data.model.OfflineFirstListingRepository
import com.tunjid.listing.data.model.OfflineFirstMediaRepository
import com.tunjid.listing.data.model.OfflineFirstUserRepository
import com.tunjid.listing.data.model.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    fun bindListingRepository(
        repository: OfflineFirstListingRepository
    ): ListingRepository

    @Binds
    fun bindMediaRepository(
        repository: OfflineFirstMediaRepository
    ): MediaRepository

    @Binds
    fun bindUserRepository(
        repository: OfflineFirstUserRepository
    ): UserRepository

    @Binds
    fun bindFavoriteRepository(
        repository: OfflineFirstFavoriteRepository
    ): FavoriteRepository
}