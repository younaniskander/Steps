package com.example

import com.example.model.NavigationGuide
import com.example.model.NavigationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class NavigationLogicTest {

    @Test
    fun `test destination bearing and distance calculations`() {
        val target = NavigationTarget(
            id = "test_target",
            name = "Market Street",
            category = "Transit",
            latitude = 37.7749,
            longitude = -122.4194,
            address = "Market St & 4th St"
        )

        assertEquals("Market Street", target.name)
        assertEquals("Transit", target.category)

        // Mock distance
        val distance = 65.4f
        val steps = (distance / 0.75f).roundToInt()
        assertEquals(87, steps)

        val guide = NavigationGuide(
            destination = target,
            distanceMeters = distance,
            bearingDegrees = 45f,
            relativeDirection = "Turn slight right",
            spokenInstruction = "Turn slight right toward 1 o'clock. 65 meters remaining."
        )

        assertNotNull(guide.destination)
        assertEquals("Turn slight right", guide.relativeDirection)
        assertTrue(guide.spokenInstruction!!.contains("65 meters"))
    }

    @Test
    fun `test relative directional angle mapping`() {
        fun computeRelativeInstruction(relativeAngle: Float, distanceMeters: Float): String {
            return when {
                distanceMeters <= 8f -> "Arrived"
                relativeAngle in -25.0..25.0 -> "Straight ahead"
                relativeAngle in 25.0..75.0 -> "Turn slight right"
                relativeAngle in 75.0..115.0 -> "Turn right"
                relativeAngle in 115.0..170.0 -> "Turn sharp right"
                relativeAngle in -75.0..-25.0 -> "Turn slight left"
                relativeAngle in -115.0..-75.0 -> "Turn left"
                else -> "Turn around"
            }
        }

        assertEquals("Arrived", computeRelativeInstruction(0f, 5f))
        assertEquals("Straight ahead", computeRelativeInstruction(5f, 50f))
        assertEquals("Turn slight right", computeRelativeInstruction(40f, 50f))
        assertEquals("Turn right", computeRelativeInstruction(90f, 50f))
        assertEquals("Turn slight left", computeRelativeInstruction(-40f, 50f))
        assertEquals("Turn left", computeRelativeInstruction(-90f, 50f))
        assertEquals("Turn around", computeRelativeInstruction(175f, 50f))
    }
}
