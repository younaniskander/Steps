package com.example.model

enum class HazardUrgency {
    CRITICAL, // Immediate stop/hazard (stairs down, dropoff, incoming vehicle)
    CAUTION,  // Obstacle ahead (curb, pole, bicycle, uneven ground)
    INFO,     // Informational (approaching door, intersection, crosswalk)
    CLEAR     // Path is clear
}

data class ObstacleAssessment(
    val hasHazard: Boolean = false,
    val hazardType: String = "Path Clear",
    val clockPosition: String = "12 o'clock",
    val distanceEstimate: String = "Clear ahead",
    val speechPrompt: String = "Path is clear ahead.",
    val recommendation: String = "Continue walking straight",
    val urgency: HazardUrgency = HazardUrgency.CLEAR,
    val timestamp: Long = System.currentTimeMillis()
)
