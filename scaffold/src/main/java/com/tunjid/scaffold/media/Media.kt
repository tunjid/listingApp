package com.tunjid.scaffold.media

import android.webkit.MimeTypeMap
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
                contentScale = mediaArgs.contentScale
            )

            mimeType.startsWith("video") -> VideoPlayer(
                modifier = Modifier.fillMaxSize(),
                url = url
            )
        }
    }
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