package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.HazardUrgency
import com.example.model.ObstacleAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StepSight", appName)
  }

  @Test
  fun `obstacle assessment default values are safe`() {
    val assessment = ObstacleAssessment()
    assertFalse(assessment.hasHazard)
    assertEquals(HazardUrgency.CLEAR, assessment.urgency)
  }
}
