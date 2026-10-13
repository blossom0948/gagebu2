package com.moasseum.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Re-encodes user-picked photos as small JPEGs, stripping metadata/EXIF before upload. */
object SharedMemoryPhotoCodec {
    suspend fun encode(resolver: ContentResolver, uri: Uri): ByteArray =
        encode { resolver.openInputStream(uri) }

    internal suspend fun encode(openInputStream: () -> InputStream?): ByteArray = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStreamOpened = openInputStream()?.use {
            BitmapFactory.decodeStream(it, null, bounds)
            true
        } ?: false
        check(boundsStreamOpened) { "선택한 사진을 열 수 없어요." }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "이미지 파일을 읽을 수 없어요." }

        val maxDimension = 1800
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        var bitmap = openInputStream()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            })
        } ?: error("선택한 사진을 열 수 없어요.")

        try {
            val orientation = openInputStream()?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrixValues = when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> floatArrayOf(-1f, 0f, bitmap.width - 1f, 0f, 1f, 0f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> floatArrayOf(-1f, 0f, bitmap.width - 1f, 0f, -1f, bitmap.height - 1f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> floatArrayOf(1f, 0f, 0f, 0f, -1f, bitmap.height - 1f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_ROTATE_90 -> floatArrayOf(0f, -1f, bitmap.height - 1f, 1f, 0f, 0f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_TRANSVERSE -> floatArrayOf(0f, -1f, bitmap.height - 1f, -1f, 0f, bitmap.width - 1f, 0f, 0f, 1f)
                ExifInterface.ORIENTATION_ROTATE_270 -> floatArrayOf(0f, 1f, 0f, -1f, 0f, bitmap.width - 1f, 0f, 0f, 1f)
                else -> null
            }
            val matrix = Matrix().apply { matrixValues?.let(::setValues) }
            if (!matrix.isIdentity) {
                val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (oriented !== bitmap) {
                    bitmap.recycle()
                    bitmap = oriented
                }
            }

            var quality = 88
            while (true) {
                val output = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "사진을 변환하지 못했어요." }
                val bytes = output.toByteArray()
                if (bytes.size <= SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES) return@withContext bytes
                when {
                    quality > 58 -> quality -= 10
                    bitmap.width > 720 || bitmap.height > 720 -> {
                        val resized = Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * 0.8f).toInt().coerceAtLeast(1),
                            (bitmap.height * 0.8f).toInt().coerceAtLeast(1),
                            true,
                        )
                        if (resized !== bitmap) {
                            bitmap.recycle()
                            bitmap = resized
                        }
                        quality = 72
                    }
                    else -> error("1MB 이하로 압축할 수 없는 사진이에요. 다른 사진을 선택해 주세요.")
                }
            }
            error("사진을 변환하지 못했어요.")
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
