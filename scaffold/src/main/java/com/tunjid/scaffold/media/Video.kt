package com.tunjid.scaffold.media

import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun Video(
    state: VideoState,
    modifier: Modifier
) {
    VideoPlayer(
        state = state,
        modifier = modifier,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    state: VideoState,
    modifier: Modifier = Modifier,
) {
    val graphicsLayer = rememberGraphicsLayer()
    val contentScale = state.contentScale.interpolate()

    Box(modifier = modifier) {
        // Note its important the embedded Surface is removed from the composition when it is scrolled
        // off screen
        if (state.canShowVideo) PlayingVideo(
            player = state.player,
            contentScale = contentScale,
            alignment = state.alignment,
            videoSize = state.videoSize,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }

        )
        if (state.canShowStill) VideoStill(
            lastBitmap = state.videoStill.takeIf {
                state.status != PlayerStatus.Idle.Initial
            },
            url = state.url,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )

        // Capture a still frame from the video to use as a stand in when buffering playback
        LaunchedEffect(state.status) {
            if (state.status is PlayerStatus.Pause
                && state.renderedFirstFrame
                && graphicsLayer.size.height != 0
                && graphicsLayer.size.width != 0
            ) {
                state.videoStill = graphicsLayer.toImageBitmap()
            }
        }
    }
    DisposableEffect(graphicsLayer) {
        state.status = PlayerStatus.Idle.Initial
        onDispose {
            state.renderedFirstFrame = false
            state.status = PlayerStatus.Idle.Evicted
        }
    }
}

@Composable
private fun VideoStill(
    lastBitmap: ImageBitmap?,
    url: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    when (lastBitmap) {
        null -> AsyncImage(
            modifier = modifier,
            model = url,
            contentDescription = null,
            contentScale = contentScale
        )

        else -> Image(
            modifier = modifier,
            bitmap = lastBitmap,
            contentDescription = null,
            contentScale = contentScale
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayingVideo(
    modifier: Modifier,
    player: Player?,
    contentScale: ContentScale,
    alignment: Alignment,
    videoSize: IntSize,
) {
    val updatedPlayer = rememberUpdatedState(player)
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    var videoMatrix by remember {
        mutableStateOf(
            value = Matrix(),
            policy = referentialEqualityPolicy()
        )
    }
    AndroidEmbeddedExternalSurface(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp))
            .onSizeChanged { surfaceSize = it },
        surfaceSize = IntSize(320, 240),
        transform = videoMatrix,
    ) {
        onSurface { createdSurface, _, _ ->
            createdSurface.onDestroyed {
                updatedPlayer.value?.setVideoSurface(null)
            }
            snapshotFlow { updatedPlayer.value }
                .filterNotNull()
                .collect {
                    it.setVideoSurface(createdSurface)
                    it.play()
                }
        }
    }

    LaunchedEffect(videoSize, surfaceSize) {
        if (videoSize.height == 0 || videoSize.width == 0) {
            return@LaunchedEffect
        }
        if (surfaceSize.height == 0 || surfaceSize.width == 0) {
            return@LaunchedEffect
        }
        videoMatrix = Matrix()
            .removeFillBounds(
                srcSize = videoSize,
                destSize = surfaceSize
            )
            .scaleAndAlignTo(
                srcSize = videoSize,
                destSize = surfaceSize,
                contentScale = contentScale,
                alignment = alignment,
            )
    }
}
