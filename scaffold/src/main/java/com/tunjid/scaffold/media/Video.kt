package com.tunjid.scaffold.media

import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs

@Composable
fun Video(
    args: VideoArgs,
    modifier: Modifier
) {
    Box(modifier) {
        when (val url = args.url) {
            null -> Box(
                modifier = Modifier.fillMaxSize(),
            )

            else -> VideoPlayer(
                modifier = Modifier.fillMaxSize(),
                url = url,
                contentScale = args.contentScale.interpolate(),
                alignment = args.alignment,
                isPlaying = args.isPlaying,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    modifier: Modifier = Modifier,
    url: String?,
    contentScale: ContentScale,
    alignment: Alignment,
    isPlaying: Boolean,
) {
    var videoSize by remember { mutableStateOf<IntSize?>(null) }
    var surfaceSize by remember { mutableStateOf<IntSize?>(null) }
    var videoMatrix by remember { mutableStateOf(Matrix()) }
    var mediaItem by remember { mutableStateOf<MediaItem?>(null) }

    val context = LocalContext.current
    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .build()
            .apply {
                repeatMode = REPEAT_MODE_ONE
            }
    }

    AndroidEmbeddedExternalSurface(
        modifier = modifier.clip(RoundedCornerShape(0.dp)),
        transform = videoMatrix,
    ) {
        onSurface { surface, _, _ ->
            player.setVideoSurface(surface)
            surface.onChanged { width, height ->
                surfaceSize = IntSize(width, height)
            }
            surface.onDestroyed {
                player.setVideoSurface(null)
            }
        }
    }

    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        mediaItem = MediaItem.fromUri(url)
    }

    LaunchedEffect(mediaItem) {
        val item = mediaItem ?: return@LaunchedEffect
        player.setMediaItem(item)
        player.prepare()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) player.play()
        else player.pause()
    }

    LaunchedEffect(videoSize, surfaceSize) {
        val currentVideoSize = videoSize
        val currentSurfaceSize = surfaceSize

        if (currentVideoSize == null || currentVideoSize.height == 0 || currentVideoSize.width == 0) {
            return@LaunchedEffect
        }
        if (currentSurfaceSize == null || currentSurfaceSize.height == 0 || currentSurfaceSize.width == 0) {
            return@LaunchedEffect
        }

        videoMatrix = Matrix()
            .removeFillBounds(
                videoSize = currentVideoSize,
                surfaceSize = currentSurfaceSize
            )
            .scaleTo(
                videoSize = currentVideoSize,
                surfaceSize = currentSurfaceSize,
                contentScale = contentScale,
                alignment = alignment,
            )
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = IntSize(size.width, size.height)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
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

data class VideoArgs(
    val url: String?,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val isPlaying: Boolean = false,
)