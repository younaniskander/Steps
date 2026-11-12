package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppMode
import com.example.model.NavigationTarget
import com.example.ui.StepSightViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewModelStateTest {

    private lateinit var app: Application
    private lateinit var viewModel: StepSightViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = StepSightViewModel(app)
    }

    @Test
    fun `test initial default state`() {
        val state = viewModel.uiState.value
        assertTrue("Scanning should be active by default", state.isScanningActive)
        assertTrue("Audio should be enabled by default", state.audioEnabled)
        assertTrue("Haptic should be enabled by default", state.hapticEnabled)
        assertEquals(3, state.scanIntervalSeconds)
        assertEquals(AppMode.WALK, state.currentMode)
        assertFalse(state.isSafetySimMode)
    }

    @Test
    fun `test toggle scanning pause and resume`() {
        viewModel.toggleScanning()
        assertFalse("Scanning should be paused", viewModel.uiState.value.isScanningActive)

        viewModel.toggleScanning()
        assertTrue("Scanning should be resumed", viewModel.uiState.value.isScanningActive)
    }

    @Test
    fun `test scan interval update`() {
        viewModel.setScanInterval(5)
        assertEquals(5, viewModel.uiState.value.scanIntervalSeconds)
    }

    @Test
    fun `test speech rate adjustment`() {
        viewModel.setSpeechRate(1.25f)
        assertEquals(1.25f, viewModel.uiState.value.speechRate)
    }

    @Test
    fun `test app mode switching`() {
        viewModel.setAppMode(AppMode.NAVIGATE)
        assertEquals(AppMode.NAVIGATE, viewModel.uiState.value.currentMode)

        viewModel.setAppMode(AppMode.EXPLORE)
        assertEquals(AppMode.EXPLORE, viewModel.uiState.value.currentMode)

        viewModel.setAppMode(AppMode.SETTINGS)
        assertEquals(AppMode.SETTINGS, viewModel.uiState.value.currentMode)

        viewModel.setAppMode(AppMode.WALK)
        assertEquals(AppMode.WALK, viewModel.uiState.value.currentMode)
    }

    @Test
    fun `test simulation scenario selection`() {
        viewModel.setSimulatedScenario("stairs_down")
        val state = viewModel.uiState.value
        assertTrue(state.isSafetySimMode)
        assertEquals("stairs_down", state.activeSimScenario)

        viewModel.setSimulatedScenario(null)
        val resetState = viewModel.uiState.value
        assertFalse(resetState.isSafetySimMode)
        assertNull(resetState.activeSimScenario)
    }

    @Test
    fun `test preset destinations availability`() {
        val presets = viewModel.presetDestinations
        assertTrue(presets.isNotEmpty())
        assertEquals(4, presets.size)
        assertEquals("Nearest Transit Station", presets[0].name)
    }
}
