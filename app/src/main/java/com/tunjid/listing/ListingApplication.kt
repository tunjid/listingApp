package com.tunjid.listing

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
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
class ListingApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var listingApp: ListingApp

    override fun onCreate() {
        super.onCreate()
        ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()

        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
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