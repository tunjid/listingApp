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
    val isEmpty: Boolean,
    val activeNav: Int = 0,
    val navigation: List<List<String>>,
    val routeStates: Map<String, ByteArray>
) : ByteSerializable

private val defaultSavedState = SavedState(
    isEmpty = true,
    navigation = listOf(
        listOf("/listings"),
        listOf("/favorites"),
        listOf("/trips"),
        listOf("/messages"),
        listOf("/profile"),
    ),
    routeStates = emptyMap()
)

interface SavedStateRepository {
    val savedState: StateFlow<SavedState>
    suspend fun saveState(savedState: SavedState)
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
        initialValue = defaultSavedState
    )

    override suspend fun saveState(savedState: SavedState) {
        dataStore.updateData { savedState }
    }
}

private class SavedStateOkioSerializer(
    private val byteSerializer: ByteSerializer
) : OkioSerializer<SavedState> {
    override val defaultValue: SavedState = defaultSavedState.copy(isEmpty = false)

    override suspend fun readFrom(source: BufferedSource): SavedState =
        byteSerializer.fromBytes(source.readByteArray())

    override suspend fun writeTo(t: SavedState, sink: BufferedSink) {
        sink.write(byteSerializer.toBytes(t))
    }
}