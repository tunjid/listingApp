package com.tunjid.listing.data.di

import android.content.Context
import com.tunjid.data.model.ByteSerializer
import com.tunjid.data.model.DelegatingByteSerializer
import com.tunjid.listing.data.model.DataStoreSavedStateRepository
import com.tunjid.listing.data.model.FavoriteRepository
import com.tunjid.listing.data.model.ListingRepository
import com.tunjid.listing.data.model.MediaRepository
import com.tunjid.listing.data.model.OfflineFirstFavoriteRepository
import com.tunjid.listing.data.model.OfflineFirstListingRepository
import com.tunjid.listing.data.model.OfflineFirstMediaRepository
import com.tunjid.listing.data.model.OfflineFirstUserRepository
import com.tunjid.listing.data.model.SavedStateRepository
import com.tunjid.listing.data.model.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Singleton

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

    @Binds
    fun bindSavedStateRepository(
        dataStoreSavedStateRepository: DataStoreSavedStateRepository
    ): SavedStateRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun savedStatePath(
        @ApplicationContext context: Context
    ): Path = context.filesDir.resolve("savedState").absolutePath.toPath()

    @Provides
    @Singleton
    fun byteSerializer(): ByteSerializer =
        DelegatingByteSerializer(
            format = ProtoBuf {
                serializersModule = SerializersModule {

                }
            }
        )
}