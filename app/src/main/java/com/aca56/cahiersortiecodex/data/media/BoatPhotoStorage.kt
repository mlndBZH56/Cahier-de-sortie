package com.aca56.cahiersortiecodex.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class BoatPhotoStorage(
    private val context: Context,
) {
    fun importCompressedPhoto(uri: Uri): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Impossible de lire l'image sélectionnée.")

        val scaledBitmap = bitmap.scaleDownIfNeeded(maxSize = 1600)
        val outputFile = File(photoDirectory(), buildFileName())

        FileOutputStream(outputFile).use { stream ->
            val compressed = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            if (!compressed) {
                error("Impossible d'enregistrer la photo compressée.")
            }
        }

        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
            scaledBitmap.recycle()
        } else {
            bitmap.recycle()
        }

        return outputFile.absolutePath
    }

    fun deletePhoto(path: String) {
        runCatching {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun photoDirectory(): File {
        val directory = File(context.filesDir, "boat_photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun buildFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        return "boat_photo_$timestamp.jpg"
    }
}

private fun Bitmap.scaleDownIfNeeded(maxSize: Int): Bitmap {
    val largestSide = max(width, height)
    if (largestSide <= maxSize) {
        return this
    }

    val scale = maxSize.toFloat() / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
