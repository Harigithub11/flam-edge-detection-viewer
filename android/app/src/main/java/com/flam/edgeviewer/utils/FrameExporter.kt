package com.flam.edgeviewer.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Frame exporter for saving processed frames to PNG
 * Handles Android scoped storage (API 29+) and legacy storage
 */
class FrameExporter(private val context: Context) {

    companion object {
        private const val TAG = "FrameExporter"
        private const val DIRECTORY_NAME = "EdgeDetectionViewer"
    }

    /**
     * Export frame to PNG
     * @param frameData Grayscale byte array
     * @param width Frame width
     * @param height Frame height
     * @param filename Optional filename
     * @return true if successful
     */
    fun exportFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
        filename: String = "frame_${System.currentTimeMillis()}.png"
    ): Boolean {
        try {
            // Convert byte array to Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // For grayscale data, convert to ARGB
            val pixels = IntArray(width * height)
            for (i in frameData.indices) {
                val gray = frameData[i].toInt() and 0xFF
                // ARGB format: (alpha << 24) | (red << 16) | (green << 8) | blue
                pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            // Save to storage based on Android version
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bitmap, filename)
            } else {
                saveToExternalStorage(bitmap, filename)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export frame", e)
            return false
        }
    }

    /**
     * Save to MediaStore (Android 10+)
     * Uses scoped storage, no permission needed
     */
    private fun saveToMediaStore(bitmap: Bitmap, filename: String): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$DIRECTORY_NAME")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Log.d(TAG, "Frame exported to: $imageUri")
                true
            } ?: false
        } ?: false
    }

    /**
     * Save to external storage (Android 9 and below)
     * Requires WRITE_EXTERNAL_STORAGE permission
     */
    private fun saveToExternalStorage(bitmap: Bitmap, filename: String): Boolean {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            DIRECTORY_NAME
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, filename)
        return FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Log.d(TAG, "Frame exported to: ${file.absolutePath}")
            true
        }
    }
}
