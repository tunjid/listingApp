package com.tunjid.listing.data.di

import com.tunjid.listing.data.model.ImageRepository
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.OfflineFirstImageRepository
import com.tunjid.listing.data.model.OfflineFirstListingRepository
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
        repository: OfflineFirstImageRepository
    ): ImageRepository

    @Binds
    fun bindUserRepository(
        repository: OfflineFirstUserRepository
    ): UserRepository
}