package com.tunjid.data.test

import com.tunjid.listing.sync.SyncManager
import com.tunjid.listing.sync.SyncStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class TestSyncManager : SyncManager {

    private val syncStatusFlow: MutableSharedFlow<SyncStatus> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val status: Flow<SyncStatus> = syncStatusFlow

    override fun requestSync() {
        syncStatusFlow.tryEmit(SyncStatus.Running)
    }

}