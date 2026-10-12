package com.moasseum.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Re-encodes user-picked photos as small JPEGs, stripping metadata/EXIF before upload. */
object SharedMemoryPhotoCodec {
    suspend fun encode(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("선택한 사진을 열 수 없어요.")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "이미지 파일을 읽을 수 없어요." }

        val maxDimension = 1800
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            })
        } ?: error("선택한 사진을 열 수 없어요.")

        try {
            val orientation = resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postScale(-1f, 1f); postRotate(90f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postScale(-1f, 1f); postRotate(270f) }
                }
            }
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
