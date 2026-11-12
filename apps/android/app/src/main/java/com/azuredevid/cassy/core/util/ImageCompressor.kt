package com.azuredevid.cassy.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Utility for compressing image bitmaps to WebP format under a target byte budget.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1280
    private const val DEFAULT_TARGET_BYTES = 300 * 1024 // 300 KB

    fun compressBytes(
        inputBytes: ByteArray,
        maxBytes: Int = DEFAULT_TARGET_BYTES,
        maxDimension: Int = MAX_DIMENSION
    ): ByteArray {
        val originalBitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
            ?: return inputBytes

        val scaledBitmap = scaleBitmap(originalBitmap, maxDimension)

        val outputStream = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        var quality = 85
        scaledBitmap.compress(format, quality, outputStream)

        while (outputStream.size() > maxBytes && quality > 40) {
            outputStream.reset()
            quality -= 10
            scaledBitmap.compress(format, quality, outputStream)
        }

        return outputStream.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide
        val targetWidth = (width * scale).roundToInt()
        val targetHeight = (height * scale).roundToInt()

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}

