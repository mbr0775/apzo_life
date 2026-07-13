package com.example.apzolife.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class AiSuggestedSimpleSubtask(
    val title: String = "",
    val description: String = ""
)

@Serializable
data class AiScheduleBlock(
    val label: String = "",
    @SerialName("duration_minutes")
    val durationMinutes: Int = 0
)

@Serializable
data class AiScheduleSuggestion(
    @SerialName("start_time")
    val startTime: String = "", // 24hr "HH:mm"

    @SerialName("total_minutes")
    val totalMinutes: Int = 0,

    val blocks: List<AiScheduleBlock> = emptyList()
)

@Serializable
data class AiSmartTaskSuggestionResult(
    val description: String = "",
    val subtasks: List<AiSuggestedSimpleSubtask> = emptyList(),

    @SerialName("schedule_suggestion")
    val scheduleSuggestion: AiScheduleSuggestion = AiScheduleSuggestion()
)

/**
 * AI Smart Task Creator ("✨ Suggest details").
 *
 * Given only a task title (and an optional draft description the user already
 * typed), this asks the model for three things in a single call: a fleshed-out
 * description, a short list of subtasks, and a realistic schedule suggestion
 * (a start time plus small study/work blocks that sum to a total duration).
 *
 * This is deliberately opt-in — triggered by a small button next to the Title
 * field, never automatic — since every call costs money and not every task
 * needs AI help. Same 4-tier fallback chain as the other AI features:
 * Gemini → Groq → Cerebras → OpenRouter, each hop triggered only on a
 * quota/rate-limit error.
 */
object AiSmartTaskSuggestionService {

    private const val TAG = "AiSmartTaskSuggester"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val blockSchema = Schema.obj(
        properties = mapOf(
            "label" to Schema.string(),
            "duration_minutes" to Schema.integer()
        )
    )

    private val scheduleSchema = Schema.obj(
        properties = mapOf(
            "start_time" to Schema.string(),
            "total_minutes" to Schema.integer(),
            "blocks" to Schema.array(blockSchema)
        )
    )

    private val subtaskSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "description" to Schema.string()
        ),
        optionalProperties = listOf("description")
    )

    private val suggestionSchema = Schema.obj(
        properties = mapOf(
            "description" to Schema.string(),
            "subtasks" to Schema.array(subtaskSchema),
            "schedule_suggestion" to scheduleSchema
        )
    )

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = suggestionSchema
                maxOutputTokens = 1280
                temperature = 0.5f
            }
        )
    }

    private val fallbackSystemPrompt = """
        You are Apzo Life's AI Smart Task Creator.
        Given a task title (and optionally a partial description), produce
        useful defaults so the user does not have to type everything by hand.
        The task can be about ANY subject: software development, study,
        fitness, business, creative work, household projects, or anything else.
        Rules:
        - description: 2-3 practical sentences expanding on what the task
          involves. If the user already gave a description, refine it instead
          of ignoring it.
        - subtasks: 3 to 6 short, concrete subtasks (title + one-line
          description each). No nested children.
        - schedule_suggestion.start_time: a sensible 24-hour "HH:mm" time to
          start today, based on the current time given below (usually soon
          after it, during normal waking hours).
        - schedule_suggestion.total_minutes: a realistic total duration for
          the whole task (typically 60-240 minutes).
        - schedule_suggestion.blocks: 2 to 5 short work/study blocks whose
          duration_minutes roughly sum to total_minutes, each block label
          matching one of the subtasks or a natural grouping of them.
        Respond ONLY with a single valid JSON object, no markdown, no commentary.
        The JSON object MUST match this exact shape:
        {
          "description": string,
          "subtasks": [ { "title": string, "description": string } ],
          "schedule_suggestion": {
            "start_time": string,
            "total_minutes": integer,
            "blocks": [ { "label": string, "duration_minutes": integer } ]
          }
        }
    """.trimIndent()

    suspend fun suggestDetails(
        title: String,
        existingDescription: String = ""
    ): Result<AiSmartTaskSuggestionResult> {
        val cleanTitle = title.trim()
        val cleanDescription = existingDescription.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a task title before using AI Suggest."))
        }

        // 1. Try Gemini first.
        val geminiResult = runCatching { callGemini(cleanTitle, cleanDescription) }
        geminiResult.onSuccess { return Result.success(it) }

        val geminiError = geminiResult.exceptionOrNull()!!
        Log.w(TAG, "Gemini failed: ${geminiError.message}", geminiError)

        if (!isQuotaOrRateLimitError(geminiError)) {
            return Result.failure(geminiError)
        }

        // 2. Gemini hit its free-tier quota — fall back to Groq.
        Log.i(TAG, "Gemini quota hit, falling back to Groq")
        val groqResult = runCatching { callGroq(cleanTitle, cleanDescription) }
        groqResult.onSuccess { return Result.success(it) }

        val groqError = groqResult.exceptionOrNull()!!
        Log.w(TAG, "Groq failed: ${groqError.message}", groqError)

        if (!isQuotaOrRateLimitError(groqError)) {
            return Result.failure(groqError)
        }

        // 3. Groq also hit its free-tier quota — fall back to Cerebras.
        Log.i(TAG, "Groq quota hit, falling back to Cerebras")
        val cerebrasResult = runCatching { callCerebras(cleanTitle, cleanDescription) }
        cerebrasResult.onSuccess { return Result.success(it) }

        val cerebrasError = cerebrasResult.exceptionOrNull()!!
        Log.w(TAG, "Cerebras failed: ${cerebrasError.message}", cerebrasError)

        if (!isQuotaOrRateLimitError(cerebrasError)) {
            return Result.failure(cerebrasError)
        }

        // 4. Cerebras ALSO hit its free-tier quota — final fallback to OpenRouter.
        Log.i(TAG, "Cerebras quota hit, falling back to OpenRouter")
        return runCatching { callOpenRouter(cleanTitle, cleanDescription) }
            .onFailure { Log.e(TAG, "OpenRouter fallback also failed", it) }
    }

    private suspend fun callGemini(title: String, description: String): AiSmartTaskSuggestionResult {
        val prompt = buildGeminiPrompt(title, description)
        val response = geminiModel.generateContent(prompt)

        val finishReason = response.candidates.firstOrNull()?.finishReason
        val responseText = response.text?.trim().orEmpty()
        Log.d(TAG, "Gemini raw response (${responseText.length} chars), finishReason=$finishReason")

        if (responseText.isBlank()) {
            error("AI returned no suggestion (finishReason=${finishReason ?: "unknown"}).")
        }

        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiSmartTaskSuggestionResult>(cleaned)
    }

    private suspend fun callGroq(title: String, description: String): AiSmartTaskSuggestionResult {
        val responseText = GroqClient.generateJson(
            systemPrompt = fallbackSystemPrompt,
            userPrompt = buildUserPrompt(title, description),
            maxTokens = 1280,
            temperature = 0.5
        )
        Log.d(TAG, "Groq raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiSmartTaskSuggestionResult>(cleaned)
    }

    private suspend fun callCerebras(title: String, description: String): AiSmartTaskSuggestionResult {
        val responseText = CerebrasClient.generateJson(
            systemPrompt = fallbackSystemPrompt,
            userPrompt = buildUserPrompt(title, description),
            maxTokens = 1280,
            temperature = 0.5
        )
        Log.d(TAG, "Cerebras raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiSmartTaskSuggestionResult>(cleaned)
    }

    private suspend fun callOpenRouter(title: String, description: String): AiSmartTaskSuggestionResult {
        val responseText = OpenRouterClient.generateJson(
            systemPrompt = fallbackSystemPrompt,
            userPrompt = buildUserPrompt(title, description),
            maxTokens = 1280,
            temperature = 0.5
        )
        Log.d(TAG, "OpenRouter raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiSmartTaskSuggestionResult>(cleaned)
    }

    private fun buildGeminiPrompt(title: String, description: String): String =
        fallbackSystemPrompt + "\n\n" + buildUserPrompt(title, description)

    private fun buildUserPrompt(title: String, description: String): String = buildString {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        appendLine("Current time right now: $now")
        appendLine("Task title: $title")
        if (description.isNotBlank()) appendLine("Existing description (refine, do not discard): $description")
    }

    private fun cleanJson(value: String): String {
        return value.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    fun friendlyErrorMessage(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("enter a task title") ->
                "Enter a task title first."
            msg.contains("network") || msg.contains("unable to resolve host") ||
                    msg.contains("timeout") ->
                "No internet connection. Check your network and try again."
            msg.contains("blocked") || msg.contains("safety") ->
                "AI couldn't process that title. Try rephrasing it."
            msg.contains("quota") || msg.contains("resource_exhausted") ||
                    msg.contains("429") || msg.contains("rate limit") ->
                "AI Suggest is busy right now. Please try again in a moment."
            msg.contains("permission") || msg.contains("unauthenticated") ||
                    msg.contains("403") || msg.contains("401") ->
                "AI Suggest is temporarily unavailable. Please try again later."
            else ->
                "DEBUG: ${e.message}" // TODO revert to "AI Suggest failed. Please try again."
        }
    }
}