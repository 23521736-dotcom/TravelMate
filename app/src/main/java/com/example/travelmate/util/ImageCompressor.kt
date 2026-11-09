package com.example.travelmate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    /**
     * Compresses an image Uri to a JPEG byte array optimized for 3GB-4GB RAM low-end devices.
     * Uses inJustDecodeBounds, RGB_565 config, downscaling to maxDimension (default 1024px),
     * and recycles bitmap immediately after compression.
     */
    suspend fun compressImageUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 1. Measure dimensions without loading image into memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

            // 2. Calculate optimal inSampleSize (power of 2)
            var inSampleSize = 1
            while (rawWidth / inSampleSize > maxDimension || rawHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decode downscaled bitmap using RGB_565 (saves 50% RAM compared to ARGB_8888)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.RGB_565
            }

            var bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 4. Smooth scale if still exceeds maxDimension
            val width = bitmap!!.width
            val height = bitmap.height
            if (width > maxDimension || height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(width, height)
                val targetW = (width * scale).toInt()
                val targetH = (height * scale).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = scaledBitmap
                }
            }

            // 5. Compress to JPEG byte array
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            // 6. Explicitly recycle bitmap to free C++ Native Heap memory immediately
            bitmap.recycle()
            System.gc()

            bytes
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error compressing image: ${e.message}", e)
            null
        }
    }

    /**
     * Saves compressed image bytes into app's private internal storage directory
     * (`context.filesDir/offline_images/`) and returns local File.
     */
    suspend fun saveToInternalStorage(
        context: Context,
        bytes: ByteArray,
        fileName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "offline_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            file
        } catch (e: Exception) {
            android.util.Log.e("ImageCompressor", "Error saving compressed image: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes temporary offline image file after successful upload.
     */
    suspend fun deleteInternalFile(file: File) = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }
}
