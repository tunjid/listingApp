package com.tunjid.scaffold.globalui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import com.tunjid.scaffold.scaffold.ListingAppState
import com.tunjid.treenav.pop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed

interface BackStatus {
    data object None : BackStatus
    data object DragDismiss : BackStatus
}

data class PreviewBackStatus(
    val touchX: Float,
    val touchY: Float,
    val progress: Float,
    val isFromLeft: Boolean,
    val isPreviewing: Boolean,
) : BackStatus

@Composable
fun BackHandler(
    enabled: Boolean,
    onStarted: () -> Unit,
    onProgressed: (BackStatus) -> Unit,
    onCancelled: () -> Unit,
    onBack: () -> Unit
) {
    PredictiveBackHandler(enabled) { progress: Flow<BackEventCompat> ->
        try {
            progress.collectIndexed { index, backEvent ->
                if (index == 0) onStarted()
                val backStatus = backEvent.toBackStatus()
                onProgressed(backStatus)
            }
            onBack()
        } catch (e: CancellationException) {
            onCancelled()
        }
    }
}

internal fun BackEventCompat.toBackStatus() = PreviewBackStatus(
    touchX = touchX,
    touchY = touchY,
    progress = progress,
    isPreviewing = progress > Float.MIN_VALUE,
    isFromLeft = swipeEdge == BackEventCompat.EDGE_LEFT
)

@Composable
fun PredictiveBackEffects(
    appState: ListingAppState,
) {
    PredictiveBackHandler(
        enabled = appState.navigation.let { it != it.pop() },
        onBack = {
            try {
                it.collect { backEvent ->
                    appState.updateGlobalUi { copy(backStatus = backEvent.toBackStatus()) }
                }
                // Dismiss back preview
                appState.updateGlobalUi { copy(backStatus = BackStatus.None) }
                // Pop navigation
                appState.pop()
            } catch (e: CancellationException) {
                appState.updateGlobalUi { copy(backStatus = BackStatus.None) }
            }
        }
    )
}

val BackStatus.touchX: Float
    get() =
        if (this is PreviewBackStatus) touchX else 0F

val BackStatus.touchY: Float
    get() =
        if (this is PreviewBackStatus) touchY else 0F

val BackStatus.progress: Float
    get() =
        if (this is PreviewBackStatus) progress else 0F

val BackStatus.isFromLeft: Boolean
    get() =
        if (this is PreviewBackStatus) isFromLeft else false

val BackStatus.isPreviewing: Boolean
    get() =
        when (this) {
            is PreviewBackStatus -> isPreviewing
            is BackStatus.DragDismiss -> true
            else -> false
        }