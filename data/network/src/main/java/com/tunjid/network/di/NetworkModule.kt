package com.tunjid.network.di

import com.tunjid.listing.data.network.BuildConfig
import com.tunjid.network.ListingNetworkDataSource
import com.tunjid.network.retrofit.RetrofitListingNetworkDatasource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }


    @Provides
    @Singleton
    fun okHttpCallFactory(): Call.Factory = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor()
                .apply {
                    if (BuildConfig.DEBUG) {
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }
                },
        )
        .build()

}

@Module
@InstallIn(SingletonComponent::class)
interface BindsNetworkModule {
    @Binds
    fun bindsDirectoryNetworkDataSource(
        networkDataSource: RetrofitListingNetworkDatasource
    ): ListingNetworkDataSource
}