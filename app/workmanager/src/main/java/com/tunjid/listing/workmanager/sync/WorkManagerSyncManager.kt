package com.tunjid.listing.workmanager.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import com.tunjid.listing.sync.SyncManager
import com.tunjid.listing.sync.SyncStatus
import com.tunjid.listing.workmanager.initializers.SyncWorkName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SyncManager] backed by [WorkInfo] from [WorkManager]
 */
@Singleton
class WorkManagerSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncManager {
    override val status: Flow<SyncStatus> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SyncWorkName)
            .map(List<WorkInfo>::status)
            .conflate()

    override fun requestSync() {
        val workManager = WorkManager.getInstance(context)
        // Run sync on app startup and ensure only one sync worker runs at any time
        workManager.enqueueUniqueWork(
            SyncWorkName,
            ExistingWorkPolicy.KEEP,
            SyncWorker.startUpSyncWork(),
        )
    }
}

private fun List<WorkInfo>.status() = when(firstOrNull()?.state) {
    null,
    State.BLOCKED,
    State.ENQUEUED -> SyncStatus.Idle
    State.RUNNING -> SyncStatus.Running
    State.SUCCEEDED -> SyncStatus.Success
    State.FAILED -> SyncStatus.Failure
    State.CANCELLED -> SyncStatus.Cancelled
}