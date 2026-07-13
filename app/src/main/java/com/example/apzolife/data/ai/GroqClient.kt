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
private data class GroqMessage(val role: String, val content: String)

@Serializable
private data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("response_format") val responseFormat: GroqResponseFormat,
    val temperature: Double = 0.5,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
private data class GroqResponseFormat(val type: String = "json_object")

@Serializable
private data class GroqChoice(val message: GroqMessage)

@Serializable
private data class GroqChatResponse(val choices: List<GroqChoice> = emptyList())

/**
 * Fallback AI backend used automatically when Gemini (Firebase AI Logic)
 * hits its free-tier quota. OpenAI-compatible API, same "return JSON" pattern.
 */
object GroqClient {

    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    const val DEFAULT_MODEL = "llama-3.3-70b-versatile"

    // encodeDefaults = true is required here: without it, kotlinx.serialization
    // silently drops fields that still hold their default value when encoding
    // the OUTGOING request body — including GroqResponseFormat.type, which is
    // "json_object" by default. That caused Groq to reject every request with
    // "'response_format.type' : property 'type' is missing".
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
        val apiKey = BuildConfig.GROQ_API_KEY
        require(apiKey.isNotBlank()) {
            "Missing GROQ_API_KEY. Add it to local.properties."
        }

        val requestBody = GroqChatRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userPrompt)
            ),
            responseFormat = GroqResponseFormat(),
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
            json.decodeFromString<GroqChatResponse>(rawBody)
        }.getOrElse {
            error("Groq request failed: $rawBody")
        }

        val content = parsed.choices.firstOrNull()?.message?.content
        return content?.trim().orEmpty().ifBlank {
            error("Groq returned an empty response: $rawBody")
        }
    }
}