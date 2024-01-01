package com.tunjid.listing.workmanager.di

import com.tunjid.listing.sync.SyncManager
import com.tunjid.listing.workmanager.sync.WorkManagerSyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {
    @Binds
    fun bindsSyncManager(
        syncStatusMonitor: WorkManagerSyncManager,
    ): SyncManager

}