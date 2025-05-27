package com.tunjid.data.database.di

import android.content.Context
import androidx.room.Room
import com.tunjid.data.database.ListingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesListingDatabase(
        @ApplicationContext context: Context,
    ): ListingDatabase = Room.databaseBuilder(
        context = context,
        klass = ListingDatabase::class.java,
        name = "listingDatabase"
    ).build()
}