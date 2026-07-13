package com.example.apzolife.data.ai

import android.util.Log
import com.example.apzolife.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * "Ask Apzo AI" chat assistant. Gemini gets real function calling via the
 * Firebase AI Logic tools API; Groq/Cerebras/OpenRouter get the equivalent
 * OpenAI-style "tools" calling via AiChatToolClient. Same 4-tier fallback
 * chain as the other AI features: Gemini -> Groq -> Cerebras -> OpenRouter,
 * each hop triggered only on a quota/rate-limit error.
 *
 * NOTE: response.functionCalls / FunctionDeclaration / FunctionResponsePart
 * below match the Firebase AI Logic Kotlin SDK's function-calling surface.
 * If your SDK version names these slightly differently, check
 * GenerateContentResponse for the equivalent function-call accessor.
 */
object AiChatService {

    private const val TAG = "AiChatAssistant"

    /**
     * Unlike runCatching, this never converts coroutine cancellation into an
     * ordinary provider failure. Resetting the chat therefore stops the chain.
     */
    private suspend fun <T> providerCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }


    /**
     * Normalizes bullet characters returned by providers before the message is
     * stored in chat history. ASCII "- " is used as the transport format;
     * ChatScreen draws the visible bullet itself.
     */
    private fun sanitizeAssistantText(value: String): String {
        val brokenBullet = "\u00E2\u20AC\u00A2"
        val doubleEncodedBullet = "\u00C3\u00A2\u00E2\u0082\u00AC\u00C2\u00A2"

        return value
            .replace(doubleEncodedBullet, "-")
            .replace(brokenBullet, "-")
            .replace("\u2022", "-")
            .lines()
            .joinToString("\n") { line ->
                val trimmed = line.trimStart()
                val indentation = line.substring(0, line.length - trimmed.length)

                when {
                    trimmed.startsWith("-") ->
                        indentation + "- " + trimmed.drop(1).trimStart()
                    trimmed.startsWith("*") ->
                        indentation + "- " + trimmed.drop(1).trimStart()
                    else -> line
                }
            }
            .trim()
    }

    // NOT const: .trimIndent() is a function call, so this cannot be a
    // compile-time constant. A regular `val` (evaluated once, lazily by the
    // object's init) is what we want here.
    private val SYSTEM_PROMPT = """
        You are Apzo AI, a friendly, concise productivity assistant built into
        the Apzo Life task tracker app. Help the user plan, prioritize, and
        manage their tasks and subtasks.

        Formatting rules (follow these strictly, like a modern AI chat app):
        - Never write a single dense paragraph.
        - Start with one short sentence (max ~12 words) that directly answers
          the question.
        - If you are listing tasks, tips, steps, or reasons, put each one on
          its own line starting with "- " (a dash and a space). Use 2 to 6
          bullets. Keep each bullet short: one idea, under ~14 words.
        - Use **double asterisks** around task titles or key terms you want
          emphasized, e.g. **Prepare for DevOps interview**.
        - Do not use headers, numbered markdown, code blocks, or emojis.
        - Total reply should read like short chat message, not an essay.

        Behavior rules:
        - Use the provided tools to look up real task data before answering
          questions like "what should I focus on" or "why am I missing
          tasks" - never guess at task data you have not fetched.
        - When asked to break a task into steps, first make sure you know the
          task's exact title (call get_today_tasks or get_pending_tasks if
          unsure), then call create_subtask once per step.
        - When asked to build a multi-day plan (e.g. "study plan for DevOps
          this week"), call create_task once per day/topic with a sensible
          start_date.
        - If a tool call fails or finds nothing, say so plainly instead of
          inventing data.
    """.trimIndent()

    /**
     * Builds the {paramName -> Schema} property map for one function's
     * parameters. FunctionDeclaration.parameters wants this map directly -
     * it is NOT a Schema.obj(...) wrapper. FunctionDeclaration builds the
     * object schema internally from this map plus optionalParameters.
     */
    private fun buildParamMap(params: List<AiChatFunctions.ParamSpec>): Map<String, Schema> {
        val properties = mutableMapOf<String, Schema>()
        for (p in params) {
            val fieldSchema: Schema = if (p.type == "integer") {
                Schema.integer(p.description)
            } else {
                Schema.string(p.description)
            }
            properties[p.name] = fieldSchema
        }
        return properties
    }

    private val chatTool by lazy {
        val declarations = AiChatFunctions.specs.map { spec ->
            FunctionDeclaration(
                name = spec.name,
                description = spec.description,
                parameters = buildParamMap(spec.params),
                optionalParameters = spec.params.filter { !it.required }.map { it.name }
            )
        }
        Tool.functionDeclarations(declarations)
    }

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash",
            tools = listOf(chatTool),
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 1400
            }
        )
    }

    suspend fun sendMessage(userMessage: String, history: List<AiChatTurn>): Result<String> {
        val geminiResult = providerCall { callGemini(userMessage, history) }
        geminiResult.onSuccess { return Result.success(it) }

        val geminiError = geminiResult.exceptionOrNull()!!
        Log.w(TAG, "Gemini failed: ${geminiError.message}", geminiError)
        if (!isQuotaOrRateLimitError(geminiError)) return Result.failure(geminiError)

        Log.i(TAG, "Gemini quota hit, falling back to Groq")
        val groqResult = providerCall {
            callFallback(GROQ_ENDPOINT, BuildConfig.GROQ_API_KEY, GroqClient.DEFAULT_MODEL, userMessage, history)
        }
        groqResult.onSuccess { return Result.success(it) }
        val groqError = groqResult.exceptionOrNull()!!
        Log.w(TAG, "Groq failed: ${groqError.message}", groqError)
        if (!isQuotaOrRateLimitError(groqError)) return Result.failure(groqError)

        Log.i(TAG, "Groq quota hit, falling back to Cerebras")
        val cerebrasResult = providerCall {
            callFallback(CEREBRAS_ENDPOINT, BuildConfig.CEREBRAS_API_KEY, CerebrasClient.DEFAULT_MODEL, userMessage, history)
        }
        cerebrasResult.onSuccess { return Result.success(it) }
        val cerebrasError = cerebrasResult.exceptionOrNull()!!
        Log.w(TAG, "Cerebras failed: ${cerebrasError.message}", cerebrasError)
        if (!isQuotaOrRateLimitError(cerebrasError)) return Result.failure(cerebrasError)

        Log.i(TAG, "Cerebras quota hit, falling back to OpenRouter")
        return providerCall {
            callFallback(
                OPENROUTER_ENDPOINT, BuildConfig.OPENROUTER_API_KEY, OpenRouterClient.DEFAULT_MODEL,
                userMessage, history,
                extraHeaders = mapOf("HTTP-Referer" to "https://tokilotech.com", "X-Title" to "Apzo Life")
            )
        }.onFailure { Log.e(TAG, "OpenRouter fallback also failed", it) }
    }

    // ---- Gemini path (Firebase AI Logic native function calling) ----
    private suspend fun callGemini(userMessage: String, history: List<AiChatTurn>): String {
        val chatHistory = buildList {
            add(content(role = "user") { text(SYSTEM_PROMPT) })
            add(content(role = "model") { text("Understood - I'll use the tools to check real task data before answering.") })
            history.takeLast(12).forEach { turn ->
                add(content(role = if (turn.role == "user") "user" else "model") { text(turn.text) })
            }
        }
        val chat = geminiModel.startChat(history = chatHistory)
        var response = chat.sendMessage(userMessage)

        var guard = 0
        while (response.functionCalls.isNotEmpty() && guard < 4) {
            // Run every suspend tool call FIRST, outside the content{} builder,
            // since content{} is a plain (non-suspend) DSL lambda and cannot
            // itself call suspend functions.
            val executedResults = response.functionCalls.map { call ->
                val argsObj = JsonObject(call.args)
                val result = AiChatFunctions.execute(call.name, argsObj)
                call.name to result
            }

            val responseContent = content(role = "function") {
                executedResults.forEach { (name, result) ->
                    part(FunctionResponsePart(name, result))
                }
            }
            response = chat.sendMessage(responseContent)
            guard++
        }

        val finishReason = response.candidates
            .firstOrNull()
            ?.finishReason
            ?.toString()
            .orEmpty()

        if (finishReason.contains("MAX_TOKENS", ignoreCase = true)) {
            error("MAX_TOKENS: Apzo AI response was cut off before completion.")
        }

        val text = response.text?.trim().orEmpty()
        if (text.isBlank()) {
            error(
                "Apzo AI returned an empty reply. " +
                        "Finish reason: ${finishReason.ifBlank { "unknown" }}"
            )
        }
        return sanitizeAssistantText(text)
    }

    // ---- OpenAI-compatible fallback path (Groq / Cerebras / OpenRouter) ----
    private suspend fun callFallback(
        endpoint: String,
        apiKey: String,
        model: String,
        userMessage: String,
        history: List<AiChatTurn>,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val messages = mutableListOf(ChatMsg(role = "system", content = SYSTEM_PROMPT))
        history.takeLast(12).forEach { turn ->
            messages.add(ChatMsg(role = if (turn.role == "user") "user" else "assistant", content = turn.text))
        }
        messages.add(ChatMsg(role = "user", content = userMessage))

        var guard = 0
        while (guard < 4) {
            val reply = AiChatToolClient.chat(endpoint, apiKey, model, messages, extraHeaders)
            val toolCalls = reply.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                val text = reply.content?.trim().orEmpty()
                if (text.isBlank()) error("Apzo AI returned an empty reply.")
                return sanitizeAssistantText(text)
            }

            messages.add(reply) // assistant message carrying the tool_calls
            toolCalls.forEach { call ->
                val argsJson = runCatching {
                    Json.parseToJsonElement(call.function.arguments).jsonObject
                }.getOrElse { buildJsonObject { } }
                val result = AiChatFunctions.execute(call.function.name, argsJson)
                messages.add(
                    ChatMsg(role = "tool", content = result.toString(), toolCallId = call.id, name = call.function.name)
                )
            }
            guard++
        }
        error("Apzo AI could not finish after multiple tool calls.")
    }

    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val CEREBRAS_ENDPOINT = "https://api.cerebras.ai/v1/chat/completions"
    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"

    fun friendlyErrorMessage(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("not logged in") -> "Please sign in to use Apzo AI."
            msg.contains("network") || msg.contains("unable to resolve host") || msg.contains("timeout") ->
                "No internet connection. Check your network and try again."
            msg.contains("blocked") || msg.contains("safety") -> "Apzo AI couldn't process that message. Try rephrasing it."
            msg.contains("max_tokens") || msg.contains("max tokens") ->
                "Apzo AI response was too long. Try a shorter request."
            msg.contains("quota") || msg.contains("resource_exhausted") || msg.contains("429") || msg.contains("rate limit") ->
                "Apzo AI is busy right now. Please try again in a moment."
            msg.contains("permission") || msg.contains("unauthenticated") || msg.contains("403") || msg.contains("401") ->
                "Apzo AI is temporarily unavailable. Please try again later."
            else -> "Apzo AI failed. Please try again."
        }
    }
}
