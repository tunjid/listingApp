package com.tunjid.scaffold.media

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.min


internal fun Matrix.scaleAndAlignTo(
    srcSize: IntSize,
    destSize: IntSize,
    contentScale: ContentScale,
    alignment: Alignment,
) = apply {
    // TextureView defaults to Fill bounds, first remove that transform
    val fillBoundsScaleFactor = ContentScale.FillBounds.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize()
    )
    scale(
        x = fillBoundsScaleFactor.scaleX,
        y = fillBoundsScaleFactor.scaleY
    )
    invert()

    // Next apply the desired contentScale
    val desiredScaleFactor = contentScale.computeScaleFactor(
        srcSize = srcSize.toSize(),
        dstSize = destSize.toSize()
    )
    scale(
        x = desiredScaleFactor.scaleX,
        y = desiredScaleFactor.scaleY
    )


    // Finally apply the desired alignment
    val scaledSrcSize = srcSize.toSize() * desiredScaleFactor

    val alignmentOffset = alignment.align(
        size = scaledSrcSize.roundToIntSize(),
        space = destSize,
        layoutDirection = LayoutDirection.Ltr,
    )

    val translationOffset = Offset(
        x = alignmentOffset.x.div(desiredScaleFactor.scaleX) * min(
            a = 1f,
            b = fillBoundsScaleFactor.scaleX
        ),
        y = alignmentOffset.y.div(desiredScaleFactor.scaleY) * min(
            a = 1f,
            b = fillBoundsScaleFactor.scaleY
        ),
    )
    println("d: $destSize; s: $scaledSrcSize; a: $alignmentOffset; o: $translationOffset; c: $desiredScaleFactor; f: $fillBoundsScaleFactor")
    val srcAspectRatio = scaledSrcSize.width / scaledSrcSize.height.toFloat()
    val destAspectRatio = destSize.width / destSize.height.toFloat()

    translate(
        x = translationOffset.x,
        y = translationOffset.y ,
    )
}
