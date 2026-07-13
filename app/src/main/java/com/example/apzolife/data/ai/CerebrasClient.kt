package com.example.apzolife.data.ai

import com.example.apzolife.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CerebrasMessage(val role: String, val content: String)

@Serializable
private data class CerebrasChatRequest(
    val model: String,
    val messages: List<CerebrasMessage>,
    @SerialName("response_format") val responseFormat: CerebrasResponseFormat,
    val temperature: Double = 0.5,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
private data class CerebrasResponseFormat(val type: String = "json_object")

@Serializable
private data class CerebrasChoice(val message: CerebrasMessage)

@Serializable
private data class CerebrasChatResponse(val choices: List<CerebrasChoice> = emptyList())

/**
 * Third (final) AI backend. Used automatically when BOTH Gemini (Firebase AI
 * Logic) AND Groq have hit their free-tier quota/rate limits. OpenAI-compatible
 * API, same request/response shape as GroqClient.
 *
 * Free tier (mid-2026): 1M tokens/day, 5 requests/min, 30K tokens/min.
 * Only "gpt-oss-120b" and "GLM-4.7" are available on the free tier.
 */
object CerebrasClient {

    private const val ENDPOINT = "https://api.cerebras.ai/v1/chat/completions"
    const val DEFAULT_MODEL = "gpt-oss-120b"

    // Same encodeDefaults = true fix that was needed for Groq applies here:
    // without it, kotlinx.serialization silently drops response_format.type
    // because "json_object" is its default value, and Cerebras (also
    // OpenAI-compatible) will reject the request the same way Groq did.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) { json(json) }
        }
    }

    suspend fun generateJson(
        systemPrompt: String,
        userPrompt: String,
        model: String = DEFAULT_MODEL,
        maxTokens: Int = 2048,
        temperature: Double = 0.5
    ): String {
        val apiKey = BuildConfig.CEREBRAS_API_KEY
        require(apiKey.isNotBlank()) {
            "Missing CEREBRAS_API_KEY. Add it to local.properties."
        }

        val requestBody = CerebrasChatRequest(
            model = model,
            messages = listOf(
                CerebrasMessage(role = "system", content = systemPrompt),
                CerebrasMessage(role = "user", content = userPrompt)
            ),
            responseFormat = CerebrasResponseFormat(),
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody)
        }

        val rawBody = response.bodyAsText()

        val parsed = runCatching {
            json.decodeFromString<CerebrasChatResponse>(rawBody)
        }.getOrElse {
            error("Cerebras request failed: $rawBody")
        }

        val content = parsed.choices.firstOrNull()?.message?.content
        return content?.trim().orEmpty().ifBlank {
            error("Cerebras returned an empty response: $rawBody")
        }
    }
}