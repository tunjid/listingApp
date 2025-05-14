package com.tunjid.scaffold.media

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                modifier = Modifier
                    .fillMaxSize()
                    .clip(args.radii.animate()),
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
    val radii: ImageCornerRadii = NoRadii
)

@Composable
fun ImageCornerRadii.animate(
    animationSpec: FiniteAnimationSpec<ImageCornerRadii> = spring(),
): RoundedCornerShape {
    val animated by animateValueAsState(
        targetValue = this,
        typeConverter = ImageCornerRadiiToVector,
        animationSpec = animationSpec,
    )

    return RoundedCornerShape(
        topStart = animated.topStart,
        topEnd = animated.topEnd,
        bottomEnd = animated.bottomEnd,
        bottomStart = animated.bottomStart
    )
}

private val NoRadii = ImageCornerRadii(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)

data class ImageCornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp,
)

private val ImageCornerRadiiToVector: TwoWayConverter<ImageCornerRadii, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = {
            AnimationVector4D(
                v1 = it.topStart.value,
                v2 = it.topEnd.value,
                v3 = it.bottomStart.value,
                v4 = it.bottomEnd.value,
            )
        },
        convertFromVector = {
            ImageCornerRadii(
                topStart = it.v1.dp,
                topEnd = it.v2.dp,
                bottomStart = it.v3.dp,
                bottomEnd = it.v4.dp
            )
        }
    )
