package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object ImageUtils {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val planes = image.planes
            val yBuffer: ByteBuffer = planes[0].buffer
            val uBuffer: ByteBuffer = planes[1].buffer
            val vBuffer: ByteBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val imageBytes = out.toByteArray()
            val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            scaleBitmapDown(rawBitmap, maxDimension = 720)
        } catch (e: Exception) {
            null
        }
    }

    fun scaleBitmapDown(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) {
            return source
        }
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Generates a realistic test scene bitmap for verifying obstacle detection
     * in emulators or safety simulation mode.
     */
    fun createSimulatedScene(scenario: String): Bitmap {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (scenario.lowercase()) {
            "stairs_down" -> {
                // Sky & wall background
                paint.color = Color.rgb(200, 215, 230)
                canvas.drawRect(0f, 0f, width.toFloat(), 180f, paint)

                // Stair steps descending
                val stepColors = listOf(
                    Color.rgb(130, 130, 135),
                    Color.rgb(100, 100, 105),
                    Color.rgb(75, 75, 80),
                    Color.rgb(50, 50, 55)
                )
                stepColors.forEachIndexed { i, color ->
                    paint.color = color
                    val top = 180f + (i * 75f)
                    canvas.drawRect(0f, top, width.toFloat(), top + 75f, paint)
                    // Yellow safety edge marker
                    paint.color = Color.rgb(255, 220, 0)
                    canvas.drawRect(0f, top, width.toFloat(), top + 10f, paint)
                }

                // Text tag
                paint.color = Color.WHITE
                paint.textSize = 28f
                canvas.drawText("CAUTION: DOWNWARD STAIRCASE 3 STEPS", 40f, 100f, paint)
            }
            "curb_dropoff" -> {
                // Sidewalk concrete
                paint.color = Color.rgb(190, 190, 195)
                canvas.drawRect(0f, 220f, width.toFloat(), height.toFloat(), paint)

                // Dark asphalt roadway ahead
                paint.color = Color.rgb(55, 55, 60)
                canvas.drawRect(0f, 0f, width.toFloat(), 220f, paint)

                // High curb edge with yellow marking
                paint.color = Color.rgb(255, 215, 0)
                canvas.drawRect(0f, 205f, width.toFloat(), 225f, paint)

                // Parked car silhouette in street
                paint.color = Color.rgb(40, 80, 160)
                canvas.drawRect(120f, 80f, 320f, 180f, paint)

                paint.color = Color.WHITE
                paint.textSize = 28f
                canvas.drawText("CURB DROPOFF & STREET CROSSING", 40f, 60f, paint)
            }
            "bicycle_hazard" -> {
                // Sidewalk
                paint.color = Color.rgb(180, 180, 180)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Parked scooter/bicycle in walking lane (1 o'clock)
                paint.color = Color.rgb(220, 40, 40)
                canvas.drawRect(360f, 140f, 480f, 380f, paint)
                paint.color = Color.BLACK
                canvas.drawCircle(390f, 380f, 40f, paint)
                canvas.drawCircle(470f, 380f, 40f, paint)

                paint.color = Color.YELLOW
                paint.textSize = 26f
                canvas.drawText("HAZARD: BICYCLE BLOCKING RIGHT PATH", 30f, 50f, paint)
            }
            else -> {
                // Clear open walkway / path
                paint.color = Color.rgb(180, 210, 240)
                canvas.drawRect(0f, 0f, width.toFloat(), 200f, paint)

                // Clean wide sidewalk extending to horizon
                paint.color = Color.rgb(210, 215, 220)
                val path = Path().apply {
                    moveTo(240f, 200f)
                    lineTo(400f, 200f)
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }
                canvas.drawPath(path, paint)

                // Green grass borders
                paint.color = Color.rgb(90, 170, 90)
                canvas.drawRect(0f, 200f, 180f, height.toFloat(), paint)
                canvas.drawRect(460f, 200f, width.toFloat(), height.toFloat(), paint)

                paint.color = Color.WHITE
                paint.textSize = 28f
                canvas.drawText("CLEAR WIDE SIDEWALK AHEAD", 40f, 80f, paint)
            }
        }

        return bitmap
    }
}
