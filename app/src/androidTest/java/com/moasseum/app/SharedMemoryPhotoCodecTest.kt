package com.moasseum.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.data.SharedMemoryPhotoCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayInputStream
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class SharedMemoryPhotoCodecTest {
    @Test
    fun localReencodeRotatesExifImageAndDropsMetadata() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Photo codec checks are restricted to the isolated QA app.", context.packageName.endsWith(".qa"))
        val captureDirectory = File(context.cacheDir, "receipt-capture").apply { mkdirs() }
        val source = File(captureDirectory, "moasseum-synthetic-exif-test.jpg")
        val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(source).use { stream ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream))
            }
            ExifInterface(source).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "synthetic QA photo")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,33/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "126/1,58/1,0/1")
                saveAttributes()
            }

            val safeJpeg = SharedMemoryPhotoCodec.encode { source.inputStream() }
            val decoded = BitmapFactory.decodeByteArray(safeJpeg, 0, safeJpeg.size)
            try {
                assertEquals(3, decoded.width)
                assertEquals(2, decoded.height)
            } finally {
                decoded.recycle()
            }

            val strippedExif = ExifInterface(ByteArrayInputStream(safeJpeg))
            assertNull(strippedExif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION))
            assertNull(strippedExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertNull(strippedExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        } finally {
            bitmap.recycle()
            source.delete()
        }
    }

    @Test
    fun localReencodeAppliesAllExifMirrorAndRotationOrientations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Photo codec checks are restricted to the isolated QA app.", context.packageName.endsWith(".qa"))
        val captureDirectory = File(context.cacheDir, "receipt-capture").apply { mkdirs() }
        val sourceWidth = 240
        val sourceHeight = 360
        val colors = listOf(
            android.graphics.Color.rgb(220, 30, 30),
            android.graphics.Color.rgb(20, 190, 40),
            android.graphics.Color.rgb(25, 55, 220),
            android.graphics.Color.rgb(230, 205, 20),
        )
        for (orientation in ExifInterface.ORIENTATION_FLIP_HORIZONTAL..ExifInterface.ORIENTATION_ROTATE_270) {
            val source = File(captureDirectory, "moasseum-synthetic-orientation-$orientation.jpg")
            val bitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888)
            try {
                for (y in 0 until sourceHeight) for (x in 0 until sourceWidth) {
                    val colorIndex = (if (y >= sourceHeight / 2) 2 else 0) + if (x >= sourceWidth / 2) 1 else 0
                    bitmap.setPixel(x, y, colors[colorIndex])
                }
                FileOutputStream(source).use { stream ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream))
                }
                ExifInterface(source).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }

                val safeJpeg = SharedMemoryPhotoCodec.encode { source.inputStream() }
                val decoded = BitmapFactory.decodeByteArray(safeJpeg, 0, safeJpeg.size)
                try {
                    val swapsDimensions = orientation in ExifInterface.ORIENTATION_TRANSPOSE..ExifInterface.ORIENTATION_ROTATE_270
                    assertEquals("width for EXIF orientation $orientation", if (swapsDimensions) sourceHeight else sourceWidth, decoded.width)
                    assertEquals("height for EXIF orientation $orientation", if (swapsDimensions) sourceWidth else sourceHeight, decoded.height)
                    val sourceCenters = listOf(
                        sourceWidth / 4 to sourceHeight / 4,
                        sourceWidth * 3 / 4 to sourceHeight / 4,
                        sourceWidth / 4 to sourceHeight * 3 / 4,
                        sourceWidth * 3 / 4 to sourceHeight * 3 / 4,
                    )
                    for ((index, point) in sourceCenters.withIndex()) {
                        val (targetX, targetY) = orientedPoint(orientation, point.first, point.second, sourceWidth, sourceHeight)
                        assertColorNear(
                            "EXIF orientation $orientation source quadrant $index",
                            colors[index],
                            decoded.getPixel(targetX, targetY),
                        )
                    }
                } finally {
                    decoded.recycle()
                }
            } finally {
                bitmap.recycle()
                source.delete()
            }
        }
    }

    private fun orientedPoint(orientation: Int, x: Int, y: Int, width: Int, height: Int): Pair<Int, Int> = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> width - 1 - x to y
        ExifInterface.ORIENTATION_ROTATE_180 -> width - 1 - x to height - 1 - y
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> x to height - 1 - y
        ExifInterface.ORIENTATION_TRANSPOSE -> y to x
        ExifInterface.ORIENTATION_ROTATE_90 -> height - 1 - y to x
        ExifInterface.ORIENTATION_TRANSVERSE -> height - 1 - y to width - 1 - x
        ExifInterface.ORIENTATION_ROTATE_270 -> y to width - 1 - x
        else -> x to y
    }

    private fun assertColorNear(message: String, expected: Int, actual: Int) {
        assertTrue("$message expected $expected but got $actual", abs(android.graphics.Color.red(expected) - android.graphics.Color.red(actual)) < 35)
        assertTrue("$message expected $expected but got $actual", abs(android.graphics.Color.green(expected) - android.graphics.Color.green(actual)) < 35)
        assertTrue("$message expected $expected but got $actual", abs(android.graphics.Color.blue(expected) - android.graphics.Color.blue(actual)) < 35)
    }
}
