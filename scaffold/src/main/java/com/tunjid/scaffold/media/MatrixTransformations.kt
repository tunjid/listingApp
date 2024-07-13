package com.tunjid.scaffold.media

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.abs

internal fun Matrix.removeFillBounds(
    srcSize: IntSize,
    destSize: IntSize,
) = apply {
    // TextureView defaults to Fill bounds, remove that transform
    val fillBounds = ContentScale.FillBounds.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize()
    )
    scale(
        x = fillBounds.scaleX,
        y = fillBounds.scaleY
    )
    invert()
}

internal fun Matrix.scaleAndAlignTo(
    srcSize: IntSize,
    destSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
) = apply {
    val scaleFactor = contentScale.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize()
    )
    scale(
        x = scaleFactor.scaleX,
        y = scaleFactor.scaleY
    )

    val scaledVideoSize = srcSize.toSize() * scaleFactor

    val alignmentOffset = alignment.align(
        size = scaledVideoSize.toIntSize(),
        space = destSize,
        layoutDirection = LayoutDirection.Ltr,
    )

    val threshold = 4f

    when {
        abs(scaledVideoSize.width - destSize.width) > threshold -> translate(
            x = alignmentOffset.x / 2f,
        )

        else -> translate(
            y = alignmentOffset.y / 2f,
        )
    }
}
