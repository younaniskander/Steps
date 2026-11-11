package com.example.service

import android.graphics.Bitmap
import android.util.Log
import com.example.model.HazardUrgency
import com.example.model.ObstacleAssessment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "StepSightAiService"

class StepSightAiService {

    private val obstacleSchema = Schema.obj(
        properties = mapOf(
            "hasHazard" to Schema.boolean(description = "True if there is an obstacle, drop-off, curb, steps, pole, vehicle, person or uneven surface in walking path"),
            "hazardType" to Schema.string(description = "Short name of obstacle e.g. Curb drop-off, Stairs down, Bicycle, Pole, Construction barrier, Clear path"),
            "clockPosition" to Schema.string(description = "Clock direction from user perspective: 12 o'clock (straight), 11 o'clock (slight left), 1 o'clock (slight right), etc."),
            "distanceEstimate" to Schema.string(description = "Estimated distance in steps or feet e.g. 2 steps away, 5 feet, 10 feet"),
            "speechPrompt" to Schema.string(description = "Concise spoken alert for Text-To-Speech, maximum 12 words. Commands safety immediately."),
            "recommendation" to Schema.string(description = "Action advice e.g. Step right, Stop, Walk straight, Veer left"),
            "urgency" to Schema.enumeration(
                values = listOf("CRITICAL", "CAUTION", "INFO", "CLEAR"),
                description = "Urgency rating"
            )
        )
    )

    private val obstacleConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = obstacleSchema
    }

    // Lazy initialization mandated by Firebase AI Logic skill
    private val obstacleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = obstacleConfig
        )
    }

    private val sceneModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun analyzeWalkingPath(bitmap: Bitmap): ObstacleAssessment {
        return try {
            val prompt = """
                You are StepSight, an assistant for a visually impaired user walking forward.
                Analyze this photo taken from their chest or hand level.
                Look specifically for:
                1. Immediate walking hazards: stairs going down/up, curbs, drop-offs, potholes, poles, tree roots, scooters, bicycles, walls.
                2. People or moving vehicles ahead.
                3. Whether the path directly in front is completely clear.
                Always prioritize physical safety. Be concise and accurate.
            """.trimIndent()

            val contentPayload = content {
                text(prompt)
                image(bitmap)
            }

            val response: GenerateContentResponse = withTimeout(40_000L) {
                obstacleModel.generateContent(contentPayload)
            }

            val jsonText = response.text ?: throw IllegalStateException("Model returned empty text.")
            Log.d(TAG, "Obstacle JSON: $jsonText")

            val json = JSONObject(jsonText)
            val hasHazard = json.optBoolean("hasHazard", false)
            val hazardType = json.optString("hazardType", if (hasHazard) "Obstacle" else "Clear Path")
            val clockPosition = json.optString("clockPosition", "12 o'clock")
            val distanceEstimate = json.optString("distanceEstimate", "Few feet")
            val speechPrompt = json.optString(
                "speechPrompt",
                if (hasHazard) "Caution: $hazardType at $clockPosition" else "Path clear ahead."
            )
            val recommendation = json.optString("recommendation", "Proceed with care")
            val urgencyString = json.optString("urgency", if (hasHazard) "CAUTION" else "CLEAR")

            val urgency = try {
                HazardUrgency.valueOf(urgencyString.uppercase())
            } catch (e: Exception) {
                if (hasHazard) HazardUrgency.CAUTION else HazardUrgency.CLEAR
            }

            ObstacleAssessment(
                hasHazard = hasHazard,
                hazardType = hazardType,
                clockPosition = clockPosition,
                distanceEstimate = distanceEstimate,
                speechPrompt = speechPrompt,
                recommendation = recommendation,
                urgency = urgency
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing walking path", e)
            ObstacleAssessment(
                hasHazard = false,
                hazardType = "Analysis Unavailable",
                clockPosition = "12 o'clock",
                distanceEstimate = "Unknown",
                speechPrompt = "Camera scan could not complete. Proceed cautiously.",
                recommendation = "Use cane or slow steps",
                urgency = HazardUrgency.INFO
            )
        }
    }

    suspend fun describeSurroundings(bitmap: Bitmap): String {
        return try {
            val prompt = """
                You are assisting a blind user. Describe what is visible in front of them in 2 to 3 sentences.
                Highlight:
                - Room or street environment (indoors vs outdoors, hallway, sidewalk, intersection).
                - Entrances, doors, stairs, crosswalks, or storefronts.
                - Readable text, room numbers, or street signs visible in the scene.
                Speak in a warm, direct tone as if you are their sighted guide.
            """.trimIndent()

            val contentPayload = content {
                text(prompt)
                image(bitmap)
            }

            val response: GenerateContentResponse = withTimeout(45_000L) {
                sceneModel.generateContent(contentPayload)
            }
            response.text ?: "No description available for this scene."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error describing surroundings", e)
            "Unable to analyze surroundings: ${e.localizedMessage ?: "Network or attestation error"}"
        }
    }
}
