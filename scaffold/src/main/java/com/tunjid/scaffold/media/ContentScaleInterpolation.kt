package com.tunjid.scaffold.media

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.lerp

@Composable
internal fun ContentScale.interpolate(): ContentScale {
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
                stiffness = Spring.StiffnessMedium
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