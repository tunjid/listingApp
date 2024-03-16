package com.tunjid.scaffold.media

import android.webkit.MimeTypeMap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage

@Composable
fun Media(
    mediaArgs: MediaArgs,
    modifier: Modifier
) {
    val url = mediaArgs.url ?: return
    val mimeType = mediaArgs.url.mimeType() ?: return

    Box(modifier) {
        when {
            mimeType.startsWith("image") -> AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = url,
                contentDescription = null,
                contentScale = mediaArgs.contentScale.interpolate()
            )

            mimeType.startsWith("video") -> VideoPlayer(
                modifier = Modifier.fillMaxSize(),
                url = url
            )
        }
    }
}

@Composable
private fun ContentScale.interpolate(): ContentScale {
    var interpolation by remember {
        mutableFloatStateOf(1f)
    }
    var previousScale by remember {
        mutableStateOf(this)
    }

    val currentScale by remember {
        mutableStateOf(this)
    }.apply {
        if (value != this@interpolate) previousScale = when {
            interpolation == 1f -> value
            else -> CapturedContentScale(
                capturedInterpolation = interpolation,
                previousScale = previousScale,
                currentScale = value
            )
        }.also { interpolation = 0f }
        value = this@interpolate
    }

    LaunchedEffect(currentScale) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow
            ),
            block = { progress, _ ->
                interpolation = progress
            },
        )
    }

    return remember {
        object : ContentScale {
            override fun computeScaleFactor(
                srcSize: Size,
                dstSize: Size
            ): ScaleFactor {
                val start = previousScale.computeScaleFactor(
                    srcSize = srcSize,
                    dstSize = dstSize
                )
                val stop = currentScale.computeScaleFactor(
                    srcSize = srcSize,
                    dstSize = dstSize
                )

                return if (start == stop) stop
                else lerp(
                    start = start,
                    stop = stop,
                    fraction = interpolation
                )
            }
        }
    }
}

private class CapturedContentScale(
    private val capturedInterpolation: Float,
    private val previousScale: ContentScale,
    private val currentScale: ContentScale,

    ) : ContentScale {
    override fun computeScaleFactor(
        srcSize: Size,
        dstSize: Size
    ): ScaleFactor = lerp(
        start = previousScale.computeScaleFactor(
            srcSize = srcSize,
            dstSize = dstSize
        ),
        stop = currentScale.computeScaleFactor(
            srcSize = srcSize,
            dstSize = dstSize
        ),
        fraction = capturedInterpolation
    )
}

fun String.mimeType(): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(this) ?: return null
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
}

@Composable
private fun VideoPlayer(
    modifier: Modifier = Modifier,
    url: String?
) {
    var hasSurface by remember { mutableStateOf(false) }
    var mediaItem by remember { mutableStateOf<MediaItem?>(null) }

    val context = LocalContext.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        mediaItem = MediaItem.fromUri(url)
    }

    LaunchedEffect(hasSurface, mediaItem) {
        val item = mediaItem?.takeIf { hasSurface } ?: return@LaunchedEffect
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    AndroidExternalSurface(
        modifier = modifier
    ) {
        onSurface { surface, _, _ ->
            player.setVideoSurface(surface)
            hasSurface = true
        }
    }
}

data class MediaArgs(
    val url: String?,
    val description: String? = null,
    val contentScale: ContentScale
)