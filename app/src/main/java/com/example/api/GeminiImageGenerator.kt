package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiImageGenerator {
    private const val TAG = "GeminiImageGenerator"
    private const val MODEL = "gemini-3-pro-image-preview"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates an image using Gemini API gemini-3-pro-image-preview model.
     * Returns the Base64 string of the image, or null if it fails.
     */
    suspend fun generateImage(prompt: String, size: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or placeholder!")
            return null
        }

        try {
            // Build the JSON request body
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfigObj = JSONObject().apply {
                    val imageConfigObj = JSONObject().apply {
                        put("aspectRatio", "1:1")
                        put("imageSize", size) // "1K", "2K", "4K"
                    }
                    put("imageConfig", imageConfigObj)

                    val responseModalitiesArray = JSONArray().apply {
                        put("IMAGE")
                    }
                    put("responseModalities", responseModalitiesArray)
                }
                put("generationConfig", generationConfigObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API Call failed: Code ${response.code}, Body: $errorBody")
                    return null
                }

                val responseBodyStr = response.body?.string() ?: return null
                val responseJson = JSONObject(responseBodyStr)

                val candidates = responseJson.optJSONArray("candidates") ?: return null
                if (candidates.length() == 0) return null

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content") ?: return null
                val parts = content.optJSONArray("parts") ?: return null
                if (parts.length() == 0) return null

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val mimeType = inlineData.optString("mimeType")
                        val base64Data = inlineData.optString("data")
                        if (base64Data.isNotEmpty()) {
                            Log.d(TAG, "Successfully generated image! MIME: $mimeType")
                            return base64Data
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during image generation", e)
        }
        return null
    }
}
