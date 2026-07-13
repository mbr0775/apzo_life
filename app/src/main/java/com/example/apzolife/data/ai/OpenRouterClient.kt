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
private data class OpenRouterMessage(val role: String, val content: String)

@Serializable
private data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @SerialName("response_format") val responseFormat: OpenRouterResponseFormat,
    val temperature: Double = 0.5,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
private data class OpenRouterResponseFormat(val type: String = "json_object")

@Serializable
private data class OpenRouterChoice(val message: OpenRouterMessage)

@Serializable
private data class OpenRouterChatResponse(val choices: List<OpenRouterChoice> = emptyList())

/**
 * Fourth (final) AI backend. Used automatically when Gemini, Groq, AND
 * Cerebras have ALL hit their free-tier quota/rate limits. OpenAI-compatible
 * API that aggregates 25+ free-tier models behind one endpoint, so it's more
 * resilient than a single-model fallback — if one free model is degraded,
 * you can swap DEFAULT_MODEL without touching request/response shapes.
 *
 * Free tier (mid-2026): 20 requests/min, 50 requests/day (or 1,000/day if
 * the account has ever added $10+ in credits, which never expire).
 */
object OpenRouterClient {

    private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    const val DEFAULT_MODEL = "meta-llama/llama-3.3-70b-instruct:free"

    // Not every free model on OpenRouter honors response_format strictly,
    // so cleanJson() in the calling service (which strips ```json fences)
    // still matters here even more than it does for Groq/Cerebras.
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
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        require(apiKey.isNotBlank()) {
            "Missing OPENROUTER_API_KEY. Add it to local.properties."
        }

        val requestBody = OpenRouterChatRequest(
            model = model,
            messages = listOf(
                OpenRouterMessage(role = "system", content = systemPrompt),
                OpenRouterMessage(role = "user", content = userPrompt)
            ),
            responseFormat = OpenRouterResponseFormat(),
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            // Optional but recommended by OpenRouter for app attribution/analytics.
            header("HTTP-Referer", "https://tokilotech.com")
            header("X-Title", "Apzo Life")
            setBody(requestBody)
        }

        val rawBody = response.bodyAsText()

        val parsed = runCatching {
            json.decodeFromString<OpenRouterChatResponse>(rawBody)
        }.getOrElse {
            error("OpenRouter request failed: $rawBody")
        }

        val content = parsed.choices.firstOrNull()?.message?.content
        return content?.trim().orEmpty().ifBlank {
            error("OpenRouter returned an empty response: $rawBody")
        }
    }
}