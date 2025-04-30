package com.tunjid.scaffold.savedstate

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tunjid.scaffold.ByteSerializable
import com.tunjid.scaffold.ByteSerializer
import com.tunjid.scaffold.fromBytes
import com.tunjid.scaffold.toBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SavedState(
    val navigation: Navigation,
) : ByteSerializable {
    @Serializable
    data class Navigation(
        val activeNav: Int = 0,
        val backStacks: List<List<String>> = emptyList(),
    )
}

val InitialSavedState = SavedState(
    navigation = SavedState.Navigation(activeNav = -1),
)

val EmptySavedState = SavedState(
    navigation = SavedState.Navigation(activeNav = 0),
)

interface SavedStateRepository {
    val savedState: StateFlow<SavedState>
    suspend fun updateState(update: SavedState.() -> SavedState)
}

@Singleton
class DataStoreSavedStateRepository @Inject constructor(
    path: Path,
    appScope: CoroutineScope,
    byteSerializer: ByteSerializer
) : SavedStateRepository {

    private val dataStore: DataStore<SavedState> = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = SavedStateOkioSerializer(byteSerializer),
            producePath = { path }
        ),
        scope = appScope
    )

    override val savedState = dataStore.data.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = InitialSavedState,
    )

    override suspend fun updateState(update: SavedState.() -> SavedState) {
        dataStore.updateData(update)
    }
}

private class SavedStateOkioSerializer(
    private val byteSerializer: ByteSerializer
) : OkioSerializer<SavedState> {
    override val defaultValue: SavedState = EmptySavedState

    override suspend fun readFrom(source: BufferedSource): SavedState =
        byteSerializer.fromBytes(source.readByteArray())

    override suspend fun writeTo(t: SavedState, sink: BufferedSink) {
        sink.write(byteSerializer.toBytes(t))
    }
}