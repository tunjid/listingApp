package com.tunjid.scaffold.globalui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tunjid.scaffold.navigation.NavigationStateHolder
import com.tunjid.treenav.pop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

fun ComponentActivity.integrateBackActions(
    globalUiStateHolder: GlobalUiStateHolder,
    navStateHolder: NavigationStateHolder,
) {
    val backPressedCallback = BackPreviewBackPressedCallback(
        globalUiStateHolder = globalUiStateHolder,
        navStateHolder = navStateHolder
    )
    onBackPressedDispatcher.addCallback(backPressedCallback)

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            navStateHolder.state.map { navState ->
                // If the nav stack can be popped, then this callback is enabled
                navState != navState.pop()
            }
                .distinctUntilChanged()
                .collect(backPressedCallback::isEnabled::set)
        }
    }
}

private class BackPreviewBackPressedCallback(
    private val globalUiStateHolder: GlobalUiStateHolder,
    private val navStateHolder: NavigationStateHolder,
) : OnBackPressedCallback(true) {
    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        globalUiStateHolder.accept { copy(backStatus = backEvent.toBackStatus()) }
    }

    override fun handleOnBackPressed() {
        // Dismiss back preview
        globalUiStateHolder.accept {
            copy(backStatus = BackStatus.None)
        }
        // Pop navigation
        navStateHolder.accept { navState.pop() }
    }

    override fun handleOnBackCancelled() {
        globalUiStateHolder.accept {
            copy(backStatus = BackStatus.None)
        }
    }
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