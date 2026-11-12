package com.example

import com.example.util.ImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImageSimulatorTest {

    @Test
    fun `test simulation scene generation for all scenarios`() {
        val scenarios = listOf("stairs_down", "curb_dropoff", "bicycle_hazard", "clear_path")
        for (scenario in scenarios) {
            val bitmap = ImageUtils.createSimulatedScene(scenario)
            assertNotNull("Bitmap should not be null for $scenario", bitmap)
            assertEquals(640, bitmap.width)
            assertEquals(480, bitmap.height)
        }
    }

    @Test
    fun `test bitmap downscaling for network optimization`() {
        val original = ImageUtils.createSimulatedScene("curb_dropoff")
        val downscaled = ImageUtils.scaleBitmapDown(original, maxDimension = 320)
        assertNotNull(downscaled)
        assertTrue(downscaled.width <= 320)
        assertTrue(downscaled.height <= 320)
    }
}
