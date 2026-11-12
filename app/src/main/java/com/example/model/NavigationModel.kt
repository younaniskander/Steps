package com.example.model

enum class AppMode {
    WALK,        // Live camera obstacle scanning and quick alerts
    NAVIGATE,    // Turn-by-turn walking guidance to destination
    EXPLORE,     // Deep environment scene description
    SETTINGS     // Accessibility, speech rate, haptic settings
}

enum class AIModelEngine(val displayName: String, val description: String) {
    GEMINI_3_8_FLASH("Gemini 3.8 Flash", "Built-in Google Multimodal AI for live obstacle detection"),
    NEXT_OCR_THELAMAPI("Next-OCR (thelamapi/next-ocr)", "Specialized 8B Vision-Language OCR model for text & signs")
}

data class NavigationTarget(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

data class NavigationGuide(
    val destination: NavigationTarget?,
    val distanceMeters: Float?,
    val bearingDegrees: Float?,
    val relativeDirection: String?,
    val spokenInstruction: String?
)
