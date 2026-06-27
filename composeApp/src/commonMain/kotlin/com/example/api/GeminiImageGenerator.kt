package com.example.api

import com.example.getApiKey
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GeminiContent(
    val contents: List<GeminiPart>,
    val generationConfig: GeminiConfig
)

@Serializable
data class GeminiPart(
    val parts: List<GeminiTextPart>
)

@Serializable
data class GeminiTextPart(
    val text: String
)

@Serializable
data class GeminiConfig(
    val imageConfig: GeminiImageConfig,
    val responseModalities: List<String>
)

@Serializable
data class GeminiImageConfig(
    val aspectRatio: String = "1:1",
    val imageSize: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiResponseContent? = null
)

@Serializable
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null
)

@Serializable
data class GeminiResponsePart(
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

object GeminiImageGenerator {
    private const val MODEL = "gemini-3-pro-image-preview"
    private const val TAG = "GeminiImageGenerator"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun generateImage(prompt: String, size: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Napier.e("API Key is missing or placeholder!", tag = TAG)
            return null
        }

        return try {
            val requestBody = GeminiContent(
                contents = listOf(
                    GeminiPart(
                        parts = listOf(GeminiTextPart(text = prompt))
                    )
                ),
                generationConfig = GeminiConfig(
                    imageConfig = GeminiImageConfig(imageSize = size),
                    responseModalities = listOf("IMAGE")
                )
            )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"

            val response: GeminiResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val candidate = response.candidates?.firstOrNull() ?: return null
            val parts = candidate.content?.parts ?: return null
            val inlineData = parts.firstNotNullOfOrNull { it.inlineData }
            inlineData?.data
        } catch (e: Exception) {
            Napier.e("Exception during image generation", tag = TAG, throwable = e)
            null
        }
    }
}
