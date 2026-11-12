package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "NextOcrService"

class NextOcrService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Executes multimodal OCR & visual question answering using thelamapi/next-ocr.
     * Supports Hugging Face Inference API or custom local/cloud endpoint running the Python model.
     */
    suspend fun processImage(
        bitmap: Bitmap,
        prompt: String = "Read all text, signs, labels, doors, and describe key visual elements.",
        apiToken: String,
        endpointUrl: String = DEFAULT_ENDPOINT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)
            val dataUrl = "data:image/jpeg;base64,$base64Image"

            // Construct payload matching the Python transformers chat template structure:
            // messages = [{"role": "user", "content": [{"type": "image", ...}, {"type": "text", ...}]}]
            val payload = JSONObject().apply {
                put("model", "thelamapi/next-ocr")
                put("max_tokens", 256)

                val messages = JSONArray()
                val userMessage = JSONObject().apply {
                    put("role", "user")
                    val contentArray = JSONArray().apply {
                        // Image part
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", dataUrl)
                            })
                        })
                        // Text prompt part
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                    }
                    put("content", contentArray)
                }
                messages.put(userMessage)
                put("messages", messages)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url(endpointUrl.ifBlank { DEFAULT_ENDPOINT })
                .post(requestBody)

            if (apiToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiToken")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val errorMsg = "Next-OCR API error (${response.code}): ${responseBody ?: response.message}"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            if (responseBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Next-OCR returned an empty response."))
            }

            // Parse response (supports OpenAI-compatible chat format or direct HF text output)
            val extractedText = parseResponse(responseBody)
            Result.success(extractedText)

        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Next-OCR model", e)
            Result.failure(e)
        }
    }

    private fun parseResponse(jsonString: String): String {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                val first = array.optJSONObject(0)
                first?.optString("generated_text")
                    ?: first?.optString("text")
                    ?: array.getString(0)
            } else {
                val json = JSONObject(trimmed)
                // Chat completions format: choices[0].message.content
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    message?.optString("content") ?: choices.getJSONObject(0).optString("text")
                } else {
                    json.optString("generated_text", json.optString("text", trimmed))
                }
            }
        } catch (e: Exception) {
            jsonString
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress JPEG to keep payload light for API transmission
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://router.huggingface.co/hf-inference/models/thelamapi/next-ocr/v1/chat/completions"
        const val HF_LEGACY_ENDPOINT = "https://api-inference.huggingface.co/models/thelamapi/next-ocr"
    }
}
