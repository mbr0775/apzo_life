package com.example.apzolife.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
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
import kotlinx.serialization.json.*

@Serializable
data class ToolCallFunction(val name: String, val arguments: String)

@Serializable
data class ToolCall(val id: String, val type: String = "function", val function: ToolCallFunction)

@Serializable
data class ChatMsg(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null
)

@Serializable
private data class ToolChoiceResp(val message: ChatMsg)

@Serializable
private data class ToolChatResponse(val choices: List<ToolChoiceResp> = emptyList())

/**
 * Generic OpenAI-compatible "tools" (function calling) client shared by the
 * Groq / Cerebras / OpenRouter fallback tiers of Ask Apzo AI.
 *
 * IMPORTANT - null handling: outgoing chat messages are built by hand as
 * JsonObject (see chatMsgToJson) rather than via the auto-generated
 * ChatMsg serializer. This is deliberate: encodeDefaults = true (needed so
 * fields like response_format.type / tools aren't silently dropped, see
 * GroqClient/CerebrasClient/OpenRouterClient) also forces every nullable
 * field on ChatMsg (name, tool_call_id, content) to be written out as an
 * explicit JSON `null` when absent. Some providers (Groq in particular)
 * reject a field that is present-but-null even though they're fine with
 * the field being entirely absent - e.g. "'messages.0.name' : Value is not
 * nullable" for a system message that never set `name`. Hand-building the
 * message JSON lets us omit null fields entirely, satisfying strict
 * providers, while still keeping encodeDefaults = true for the parts of
 * the request (tools/response_format) that actually need it.
 */
object AiChatToolClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000L
                requestTimeoutMillis = 40_000L
                socketTimeoutMillis = 40_000L
            }
        }
    }

    private fun buildToolDefs(): JsonArray = buildJsonArray {
        AiChatFunctions.specs.forEach { spec ->
            add(buildJsonObject {
                put("type", "function")
                putJsonObject("function") {
                    put("name", spec.name)
                    put("description", spec.description)
                    putJsonObject("parameters") {
                        put("type", "object")
                        putJsonObject("properties") {
                            spec.params.forEach { p ->
                                putJsonObject(p.name) {
                                    put("type", p.type)
                                    put("description", p.description)
                                }
                            }
                        }
                        putJsonArray("required") {
                            spec.params.filter { it.required }.forEach { add(it.name) }
                        }
                    }
                }
            })
        }
    }

    /**
     * Hand-builds one chat message's JSON, omitting every field that is
     * null instead of writing it out as JSON `null`. This is the actual
     * fix for the "'messages.0.name' : Value is not nullable" error.
     */
    private fun chatMsgToJson(m: ChatMsg): JsonObject = buildJsonObject {
        put("role", m.role)

        // content: OpenAI-compatible APIs generally require content to be
        // present (can be an empty string) except for assistant messages
        // that carry tool_calls, where null/absent is accepted. To stay
        // maximally compatible across Groq/Cerebras/OpenRouter, we send an
        // empty string instead of omitting content entirely when it's null
        // and there are no tool_calls; if tool_calls ARE present we omit
        // content so the assistant "I'm calling a tool" message is clean.
        if (m.toolCalls.isNullOrEmpty()) {
            put("content", m.content ?: "")
        } else {
            m.content?.let { put("content", it) }
        }

        if (!m.toolCalls.isNullOrEmpty()) {
            putJsonArray("tool_calls") {
                m.toolCalls.forEach { tc ->
                    add(buildJsonObject {
                        put("id", tc.id)
                        put("type", tc.type)
                        putJsonObject("function") {
                            put("name", tc.function.name)
                            put("arguments", tc.function.arguments)
                        }
                    })
                }
            }
        }

        m.toolCallId?.let { put("tool_call_id", it) }
        m.name?.let { put("name", it) }
    }

    suspend fun chat(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<ChatMsg>,
        extraHeaders: Map<String, String> = emptyMap(),
        maxTokens: Int = 1200,
        temperature: Double = 0.4
    ): ChatMsg {
        require(apiKey.isNotBlank()) { "Missing API key for $endpoint" }

        val requestJson = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                messages.forEach { m -> add(chatMsgToJson(m)) }
            }
            put("tools", buildToolDefs())
            put("tool_choice", "auto")
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }

        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            extraHeaders.forEach { (k, v) -> header(k, v) }
            setBody(requestJson.toString())
        }

        val rawBody = response.bodyAsText()
        val parsed = runCatching { json.decodeFromString<ToolChatResponse>(rawBody) }
            .getOrElse { error("Tool-call request failed: $rawBody") }

        return parsed.choices.firstOrNull()?.message
            ?: error("Empty tool-call response: $rawBody")
    }
}
