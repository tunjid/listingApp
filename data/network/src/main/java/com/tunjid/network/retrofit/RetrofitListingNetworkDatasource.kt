package com.tunjid.network.retrofit

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tunjid.network.ListingNetworkDataSource
import com.tunjid.network.model.NetworkListing
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://www.airbnb.com"

interface RetrofitNetworkApi {
    @GET("listing")
    suspend fun popularListings(): List<NetworkListing>
}

/**
 * [Retrofit] backed [ListingNetworkDataSource]
 */
@Singleton
class RetrofitListingNetworkDatasource @Inject constructor(
    networkJson: Json,
    okhttpCallFactory: Call.Factory,
    @ApplicationContext context: Context
) : ListingNetworkDataSource {

    private val networkApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient()
                .newBuilder()
                .addInterceptor(ResponseInterceptor(context))
//                .addInterceptor(
//                    HttpLoggingInterceptor()
//                        .apply {
//                            if (BuildConfig.DEBUG) {
//                                setLevel(HttpLoggingInterceptor.Level.BODY)
//                            }
//                        },
//                )
                .build()
        )
        .addConverterFactory(
            networkJson.asConverterFactory("application/json".toMediaType()),
        )
        .build()
        .create(RetrofitNetworkApi::class.java)

    override suspend fun popularListings(): List<NetworkListing> =
        networkApi.popularListings()
}

private class ResponseInterceptor(
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // This line is to simulate the response delay time
        Thread.sleep(RESPONSE_DELAY_TIME)
        val jsonData = context.assets.open("listings.json")
            .bufferedReader()
            .readText()

        return Response.Builder().code(200)
            .protocol(Protocol.HTTP_1_1)
            .body(jsonData.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .request(chain.request())
            .message(jsonData)
            .build()
    }
}

private const val RESPONSE_DELAY_TIME = 3000L