package com.tunjid.listing

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.tunjid.listing.workmanager.initializers.Sync
import com.tunjid.scaffold.scaffold.AppState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class ListingApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var listingApp: ListingApp

    override fun onCreate() {
        super.onCreate()
        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()
}

interface ListingApp {
    val appState: AppState
}

@Singleton
class PersistedListingApp @Inject constructor(
    override val appState: AppState,
) : ListingApp


@Module
@InstallIn(SingletonComponent::class)
interface DirectoryAppModule {
    @Binds
    fun bindDirectoryApp(app: PersistedListingApp): ListingApp
}