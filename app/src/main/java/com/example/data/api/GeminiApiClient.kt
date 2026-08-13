package com.example.data.api

import com.example.BuildConfig
import com.example.data.models.GeminiModelOption
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    companion object {
        val SUPPORTED_MODELS = listOf(
            GeminiModelOption(
                id = "gemini-3.5-flash",
                displayName = "Gemini 3.5 Flash (Recommended)",
                description = "Fast, accurate document extraction for JEE STEM papers.",
                isDefault = true
            ),
            GeminiModelOption(
                id = "gemini-3.1-flash-lite-preview",
                displayName = "Gemini 3.1 Flash-Lite",
                description = "Ultra-fast lightweight model for quick parsing.",
                isDefault = false
            ),
            GeminiModelOption(
                id = "gemini-3.1-pro-preview",
                displayName = "Gemini 3.1 Pro (Deep Math)",
                description = "High precision for complex equations and intricate diagrams.",
                isDefault = false
            )
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun testConnection(modelId: String = "gemini-3.5-flash", customApiKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getApiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing or not configured in Secrets panel.")
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Please reply with exact text: OK_JEE_CBT")
                        })
                    })
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val replyText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (replyText.isNotBlank()) {
                Result.success("✓ Gemini connected successfully")
            } else {
                Result.failure(Exception("Gemini returned an empty response."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network or connection timeout error."))
        }
    }

    suspend fun generateContentWithMultimodal(
        modelId: String,
        prompt: String,
        base64Images: List<String> = emptyList(),
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else getApiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please ensure GEMINI_API_KEY is configured.")
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        try {
            val partsArray = JSONArray()

            // Add text prompt first
            partsArray.put(JSONObject().apply {
                put("text", prompt)
            })

            // Add images if provided (for OCR / Document understanding)
            for (base64Img in base64Images) {
                partsArray.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Img)
                    })
                })
            }

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val replyText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (replyText.isNotBlank()) {
                Result.success(replyText)
            } else {
                Result.failure(Exception("Gemini returned an empty extraction result."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to process document with Gemini."))
        }
    }

    private fun parseErrorMessage(code: Int, responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: ""
            when {
                code == 400 && message.contains("API key", ignoreCase = true) -> "Invalid Gemini API key. Please check your key configuration."
                code == 429 || message.contains("quota", ignoreCase = true) -> "Gemini API quota exceeded. Please wait or check your usage tier."
                code == 404 -> "Selected model is currently unavailable or invalid."
                message.isNotBlank() -> "Gemini Error ($code): $message"
                else -> "Gemini HTTP Request failed with code $code."
            }
        } catch (e: Exception) {
            "Gemini request failed (HTTP $code)."
        }
    }
}
