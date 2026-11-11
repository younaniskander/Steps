package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppMode
import com.example.model.HazardUrgency
import com.example.model.NavigationGuide
import com.example.model.NavigationTarget
import com.example.model.ObstacleAssessment
import com.example.service.AudioHapticManager
import com.example.service.LocationAndCompassManager
import com.example.service.StepSightAiService
import com.example.util.ImageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "StepSightViewModel"

data class StepSightUiState(
    val isScanningActive: Boolean = true,
    val isProcessingFrame: Boolean = false,
    val lastObstacle: ObstacleAssessment = ObstacleAssessment(),
    val latestSpokenAlert: String = "StepSight is ready. Camera scanning active.",
    val sceneDescription: String? = null,
    val isDescribingScene: Boolean = false,
    val audioEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val scanIntervalSeconds: Int = 3,
    val currentAddress: String = "Locating...",
    val compassAzimuth: Float = 0f,
    val navigationGuide: NavigationGuide? = null,
    val searchResults: List<NavigationTarget> = emptyList(),
    val isSearching: Boolean = false,
    val currentMode: AppMode = AppMode.WALK,
    val isSafetySimMode: Boolean = false,
    val activeSimScenario: String? = null,
    val errorMessage: String? = null
)

class StepSightViewModel(application: Application) : AndroidViewModel(application) {

    private val aiService = StepSightAiService()
    val audioHapticManager = AudioHapticManager(application)
    val locationCompassManager = LocationAndCompassManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(StepSightUiState())
    val uiState: StateFlow<StepSightUiState> = _uiState.asStateFlow()

    private var latestBitmap: Bitmap? = null
    private var scanLoopJob: Job? = null

    // Preset handy destinations for quick navigation
    val presetDestinations = listOf(
        NavigationTarget("p1", "Nearest Transit Station", "Transit", 37.7749, -122.4194, "Market & 4th St"),
        NavigationTarget("p2", "Community Pharmacy", "Health", 37.7760, -122.4210, "500 Mission St"),
        NavigationTarget("p3", "City Grocery Market", "Shopping", 37.7735, -122.4180, "250 Folsom St"),
        NavigationTarget("p4", "Central Public Park", "Park", 37.7720, -122.4230, "700 Howard St")
    )

    init {
        // Collect location and compass updates
        viewModelScope.launch {
            locationCompassManager.currentAddress.collect { address ->
                _uiState.update { it.copy(currentAddress = address) }
            }
        }
        viewModelScope.launch {
            locationCompassManager.compassAzimuth.collect { azimuth ->
                _uiState.update { it.copy(compassAzimuth = azimuth) }
            }
        }
        viewModelScope.launch {
            locationCompassManager.navigationGuide.collect { guide ->
                _uiState.update { it.copy(navigationGuide = guide) }
                guide?.spokenInstruction?.let { prompt ->
                    if (guide.distanceMeters != null && guide.distanceMeters <= 8f) {
                        audioHapticManager.speak(prompt, isUrgent = true)
                        audioHapticManager.vibrateDirection()
                    }
                }
            }
        }

        locationCompassManager.startLocationUpdates()
        startPeriodicScanLoop()
    }

    fun onNewCameraFrame(bitmap: Bitmap) {
        latestBitmap = bitmap
    }

    private fun startPeriodicScanLoop() {
        scanLoopJob?.cancel()
        scanLoopJob = viewModelScope.launch {
            while (isActive) {
                val intervalMs = _uiState.value.scanIntervalSeconds * 1000L
                delay(intervalMs)

                if (!_uiState.value.isScanningActive || _uiState.value.isProcessingFrame) {
                    continue
                }

                // If in simulator mode or has actual frame
                val bitmapToScan = if (_uiState.value.isSafetySimMode && _uiState.value.activeSimScenario != null) {
                    ImageUtils.createSimulatedScene(_uiState.value.activeSimScenario!!)
                } else {
                    latestBitmap
                }

                if (bitmapToScan != null) {
                    processObstacleFrame(bitmapToScan)
                }
            }
        }
    }

    fun toggleScanning() {
        val newState = !_uiState.value.isScanningActive
        _uiState.update { it.copy(isScanningActive = newState) }
        val announcement = if (newState) "Obstacle scanning resumed" else "Obstacle scanning paused"
        audioHapticManager.speak(announcement, isUrgent = true)
        audioHapticManager.vibrateCaution()
    }

    fun triggerImmediateScan() {
        viewModelScope.launch {
            audioHapticManager.speak("Scanning path now", isUrgent = true)
            audioHapticManager.vibrateCaution()

            val bitmap = if (_uiState.value.isSafetySimMode && _uiState.value.activeSimScenario != null) {
                ImageUtils.createSimulatedScene(_uiState.value.activeSimScenario!!)
            } else {
                latestBitmap ?: ImageUtils.createSimulatedScene("clear_path")
            }
            processObstacleFrame(bitmap)
        }
    }

    private suspend fun processObstacleFrame(bitmap: Bitmap) {
        _uiState.update { it.copy(isProcessingFrame = true, errorMessage = null) }
        try {
            val assessment = aiService.analyzeWalkingPath(bitmap)
            Log.d(TAG, "Assessment received: ${assessment.speechPrompt} (${assessment.urgency})")

            _uiState.update {
                it.copy(
                    isProcessingFrame = false,
                    lastObstacle = assessment,
                    latestSpokenAlert = assessment.speechPrompt
                )
            }

            // High-priority audio & haptic output
            audioHapticManager.notifyHazard(assessment.urgency, assessment.speechPrompt)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
            _uiState.update {
                it.copy(
                    isProcessingFrame = false,
                    errorMessage = e.localizedMessage ?: "Analysis error"
                )
            }
        }
    }

    fun describeSurroundings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDescribingScene = true, errorMessage = null) }
            audioHapticManager.speak("Analyzing surrounding scene. Please hold still.", isUrgent = true)

            try {
                val bitmap = if (_uiState.value.isSafetySimMode && _uiState.value.activeSimScenario != null) {
                    ImageUtils.createSimulatedScene(_uiState.value.activeSimScenario!!)
                } else {
                    latestBitmap ?: ImageUtils.createSimulatedScene("curb_dropoff")
                }

                val description = aiService.describeSurroundings(bitmap)
                _uiState.update {
                    it.copy(
                        isDescribingScene = false,
                        sceneDescription = description
                    )
                }
                audioHapticManager.speak(description, isUrgent = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Scene description error", e)
                _uiState.update {
                    it.copy(
                        isDescribingScene = false,
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    fun setDestination(target: NavigationTarget?) {
        locationCompassManager.setDestination(target)
        if (target != null) {
            val announcement = "Navigating to ${target.name}. Turn guidance active."
            audioHapticManager.speak(announcement, isUrgent = true)
            audioHapticManager.vibrateDirection()
        } else {
            audioHapticManager.speak("Navigation stopped", isUrgent = false)
        }
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            audioHapticManager.speak("Searching for $query", isUrgent = false)
            val results = locationCompassManager.searchAddress(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
            if (results.isEmpty()) {
                audioHapticManager.speak("No locations found for $query", isUrgent = false)
            } else {
                audioHapticManager.speak("Found ${results.size} locations. Select one to navigate.", isUrgent = false)
            }
        }
    }

    fun setAppMode(mode: AppMode) {
        _uiState.update { it.copy(currentMode = mode) }
        val modeName = when (mode) {
            AppMode.WALK -> "Walking Mode"
            AppMode.NAVIGATE -> "Navigation Mode"
            AppMode.EXPLORE -> "Explore Surroundings Mode"
            AppMode.SETTINGS -> "Settings Mode"
        }
        audioHapticManager.speak("$modeName selected", isUrgent = false)
        audioHapticManager.vibrateClear()
    }

    fun setScanInterval(intervalSeconds: Int) {
        _uiState.update { it.copy(scanIntervalSeconds = intervalSeconds) }
        audioHapticManager.speak("Scan interval set to $intervalSeconds seconds", isUrgent = false)
        startPeriodicScanLoop()
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate) }
        audioHapticManager.speechRate = rate
        audioHapticManager.speak("Speech rate adjusted", isUrgent = false)
    }

    fun toggleVoice(enabled: Boolean) {
        _uiState.update { it.copy(audioEnabled = enabled) }
        audioHapticManager.isVoiceEnabled = enabled
    }

    fun toggleHaptic(enabled: Boolean) {
        _uiState.update { it.copy(hapticEnabled = enabled) }
        audioHapticManager.isHapticEnabled = enabled
        if (enabled) audioHapticManager.vibrateCaution()
    }

    fun setSimulatedScenario(scenario: String?) {
        _uiState.update {
            it.copy(
                isSafetySimMode = scenario != null,
                activeSimScenario = scenario
            )
        }
        if (scenario != null) {
            audioHapticManager.speak("Simulated scenario: $scenario active", isUrgent = true)
            triggerImmediateScan()
        } else {
            audioHapticManager.speak("Live camera mode active", isUrgent = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanLoopJob?.cancel()
        locationCompassManager.stopLocationUpdates()
        audioHapticManager.destroy()
    }
}
