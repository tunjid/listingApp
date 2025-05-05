package com.tunjid.scaffold.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.tunjid.composables.ui.animate


@Composable
fun Photo(
    args: PhotoArgs,
    modifier: Modifier
) {
    Box(modifier) {
        when (val url = args.url) {
            null -> Box(
                modifier = Modifier.fillMaxSize(),
            )

            else -> AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = url,
                contentDescription = null,
                contentScale = args.contentScale.animate()
            )
        }
    }
}

data class PhotoArgs(
    val url: String?,
    val contentScale: ContentScale,
    val alignment: Alignment = Alignment.Center,
    val description: String? = null,
)
