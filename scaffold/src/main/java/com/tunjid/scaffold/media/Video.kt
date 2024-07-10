package com.tunjid.scaffold.media

import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

@Composable
fun Video(
    args: VideoArgs,
    modifier: Modifier
) {
    VideoPlayer(
        modifier = modifier,
        url = args.url,
        contentScale = args.contentScale.interpolate(),
        alignment = args.alignment,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    modifier: Modifier = Modifier,
    url: String,
    contentScale: ContentScale,
    alignment: Alignment,
) {
    val state = LocalPlayerManager.current.stateFor(url)
    val graphicsLayer = rememberGraphicsLayer()

    if (state.canShowVideo()) PlayingVideo(
        modifier = remember(modifier) {
            modifier
                .drawWithContent {
                    // call record to capture the content in the graphics layer
                    graphicsLayer.record {
                        // draw the contents of the composable into the graphics layer
                        this@drawWithContent.drawContent()
                    }
                    // draw the graphics layer on the visible canvas
                    drawLayer(graphicsLayer)
                }
        },
        player = state.player,
        contentScale = contentScale,
        alignment = alignment,
        videoSize = state.videoSize
    )
    // Capture a still frame from the video to use as a stand in when buffering playback
    LaunchedEffect(state.status) {
        if (state.status is PlayerStatus.Pause
            && state.renderedFirstFrame
            && graphicsLayer.size.height != 0
            && graphicsLayer.size.width != 0
        ) {
            val still = graphicsLayer.toImageBitmap()
            state.videoStill = still
        }
    }


    if (state.canShowStill()) VideoStill(
        lastBitmap = state.videoStill.takeIf {
            state.status != PlayerStatus.Idle.Initial
        },
        url = url,
        modifier = modifier,
        contentScale = contentScale
    )


    Label(
        text = listOfNotNull(
            "STILL".takeIf { state.canShowStill() },
            "VIDEO".takeIf { state.canShowVideo() },
            "${state.videoSize}".takeIf { state.canShowVideo() },
            "${state.renderedFirstFrame}",
            "${state.status}",
        ).joinToString(separator = "\n"),
        modifier
    )


    DisposableEffect(Unit) {
        state.status = PlayerStatus.Idle.Initial
        onDispose {
            state.renderedFirstFrame = false
            state.status = PlayerStatus.Idle.Evicted
            state.playerPosition = 0L
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    Box(modifier = modifier) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.4f))
        )
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.4f))
        )
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
                videoSize = videoSize,
                surfaceSize = surfaceSize
            )
            .scaleTo(
                videoSize = videoSize,
                surfaceSize = surfaceSize,
                contentScale = contentScale,
                alignment = alignment,
            )
    }
}

private fun Matrix.removeFillBounds(
    videoSize: IntSize,
    surfaceSize: IntSize,
) = apply {
    // TextureView defaults to Fill bounds, remove that transform
    val fillBounds = ContentScale.FillBounds.computeScaleFactor(
        srcSize = videoSize.toSize(),
        dstSize = surfaceSize.toSize()
    )
    scale(
        x = fillBounds.scaleX,
        y = fillBounds.scaleY
    )
    invert()
}

private fun Matrix.scaleTo(
    videoSize: IntSize,
    surfaceSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
) = apply {
    val scaleFactor = contentScale.computeScaleFactor(
        srcSize = videoSize.toSize(),
        dstSize = surfaceSize.toSize()
    )
    scale(
        x = scaleFactor.scaleX,
        y = scaleFactor.scaleY
    )

    val scaledVideoSize = videoSize.toSize() * scaleFactor

    val alignmentOffset = alignment.align(
        size = scaledVideoSize.toIntSize(),
        space = surfaceSize,
        layoutDirection = LayoutDirection.Ltr,
    )

    val threshold = 4f

    when {
        abs(scaledVideoSize.width - surfaceSize.width) > threshold -> translate(
            x = alignmentOffset.x / 2f,
        )

        else -> translate(
            y = alignmentOffset.y / 2f,
        )
    }
}

private fun PlayerState.canShowVideo() = when (status) {
    is PlayerStatus.Idle.Initial -> true
    is PlayerStatus.Play -> true
    is PlayerStatus.Pause -> true
    PlayerStatus.Idle.Evicted -> false
}

private fun PlayerState.canShowStill() =
    videoSize == IntSize.Zero
            || !renderedFirstFrame
            || when (status) {
        is PlayerStatus.Idle -> true
        is PlayerStatus.Pause -> false
        PlayerStatus.Play.PlayRequested -> true
        PlayerStatus.Play.Playing -> false
    }

data class VideoArgs(
    val url: String,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
)
