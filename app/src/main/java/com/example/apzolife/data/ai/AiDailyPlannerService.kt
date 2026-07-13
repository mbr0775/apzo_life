package com.example.apzolife.data.ai

import android.util.Log
import com.example.apzolife.data.model.MainTask
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
data class AiPlannedItem(
    @SerialName("time_label")
    val timeLabel: String = "",

    @SerialName("task_id")
    val taskId: String? = null,

    val title: String = "",
    val note: String = "",

    @SerialName("duration_minutes")
    val durationMinutes: Int = 0
)

@Serializable
data class AiDailyPlanResult(
    @SerialName("plan_summary")
    val planSummary: String = "",

    val items: List<AiPlannedItem> = emptyList()
)

object AiDailyPlannerService {

    private const val TAG = "AiDailyPlanner"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val plannedItemSchema = Schema.obj(
        properties = mapOf(
            "time_label" to Schema.string(),
            "task_id" to Schema.string(),
            "title" to Schema.string(),
            "note" to Schema.string(),
            "duration_minutes" to Schema.integer()
        ),
        optionalProperties = listOf("task_id", "note", "duration_minutes")
    )

    private val dailyPlanSchema = Schema.obj(
        properties = mapOf(
            "plan_summary" to Schema.string(),
            "items" to Schema.array(plannedItemSchema)
        ),
        optionalProperties = listOf("plan_summary")
    )

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyPlanSchema
                maxOutputTokens = 1536
                temperature = 0.4f
            }
        )
    }

    private val groqSystemPrompt = """
        You are Apzo Life AI Daily Planner.
        You will be given the user's PENDING tasks for today only.
        Arrange them into a realistic, well-paced schedule for the rest of today.
        Rules:
        - Every task given MUST appear exactly once in the output, referenced by its task_id.
        - You MAY add short optional break/revision items with task_id set to null.
        - If a task has a fixed_start_time, treat it as a fixed anchor.
        - time_label should be a short clock time like "9:00 AM" or a period like "Evening".
        - note should be a one-line practical tip (or empty string).
        - Keep duration_minutes realistic (default 30-60 if unknown).
        Respond ONLY with a single valid JSON object, no markdown, no commentary.
        The JSON object MUST match this exact shape:
        {
          "plan_summary": string,
          "items": [
            { "time_label": string, "task_id": string or null, "title": string, "note": string, "duration_minutes": integer }
          ]
        }
    """.trimIndent()

    suspend fun planDay(pendingTasks: List<MainTask>): Result<AiDailyPlanResult> {
        if (pendingTasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("No pending tasks today to plan."))
        }

        // 1. Try Gemini first.
        val geminiResult = runCatching { callGemini(pendingTasks) }
        geminiResult.onSuccess { return Result.success(it) }

        val geminiError = geminiResult.exceptionOrNull()!!
        Log.w(TAG, "Gemini failed: ${geminiError.message}", geminiError)

        if (!isQuotaOrRateLimitError(geminiError)) {
            // Real error (bad prompt, blocked content, network) — don't mask it, surface it.
            return Result.failure(geminiError)
        }

        // 2. Gemini hit its free-tier quota — fall back to Groq.
        Log.i(TAG, "Gemini quota hit, falling back to Groq")
        val groqResult = runCatching { callGroq(pendingTasks) }
        groqResult.onSuccess { return Result.success(it) }

        val groqError = groqResult.exceptionOrNull()!!
        Log.w(TAG, "Groq failed: ${groqError.message}", groqError)

        if (!isQuotaOrRateLimitError(groqError)) {
            // Real Groq error (not quota) — don't waste further calls, surface it.
            return Result.failure(groqError)
        }

        // 3. Groq also hit its free-tier quota — fall back to Cerebras.
        Log.i(TAG, "Groq quota hit, falling back to Cerebras")
        val cerebrasResult = runCatching { callCerebras(pendingTasks) }
        cerebrasResult.onSuccess { return Result.success(it) }

        val cerebrasError = cerebrasResult.exceptionOrNull()!!
        Log.w(TAG, "Cerebras failed: ${cerebrasError.message}", cerebrasError)

        if (!isQuotaOrRateLimitError(cerebrasError)) {
            // Real Cerebras error (not quota) — don't waste a fourth call, surface it.
            return Result.failure(cerebrasError)
        }

        // 4. Cerebras ALSO hit its free-tier quota — final fallback to OpenRouter.
        Log.i(TAG, "Cerebras quota hit, falling back to OpenRouter")
        return runCatching { callOpenRouter(pendingTasks) }
            .onFailure { Log.e(TAG, "OpenRouter fallback also failed", it) }
    }

    private suspend fun callGemini(tasks: List<MainTask>): AiDailyPlanResult {
        val prompt = buildPrompt(tasks)
        val response = geminiModel.generateContent(prompt)

        val finishReason = response.candidates.firstOrNull()?.finishReason
        val responseText = response.text?.trim().orEmpty()
        Log.d(TAG, "Gemini plan response (${responseText.length} chars), finishReason=$finishReason")

        if (responseText.isBlank()) {
            error("AI returned no plan (finishReason=${finishReason ?: "unknown"}).")
        }

        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiDailyPlanResult>(cleaned)
    }

    private suspend fun callGroq(tasks: List<MainTask>): AiDailyPlanResult {
        val userPrompt = buildPrompt(tasks)
        val responseText = GroqClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1536,
            temperature = 0.4
        )
        Log.d(TAG, "Groq plan response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiDailyPlanResult>(cleaned)
    }

    private suspend fun callCerebras(tasks: List<MainTask>): AiDailyPlanResult {
        val userPrompt = buildPrompt(tasks)
        val responseText = CerebrasClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1536,
            temperature = 0.4
        )
        Log.d(TAG, "Cerebras plan response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiDailyPlanResult>(cleaned)
    }

    private suspend fun callOpenRouter(tasks: List<MainTask>): AiDailyPlanResult {
        val userPrompt = buildPrompt(tasks)
        val responseText = OpenRouterClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1536,
            temperature = 0.4
        )
        Log.d(TAG, "OpenRouter plan response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiDailyPlanResult>(cleaned)
    }

    private fun buildPrompt(tasks: List<MainTask>): String = buildString {
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
        appendLine("Current time right now: $now")
        appendLine()
        appendLine("Tasks:")
        tasks.forEach { task ->
            append("- task_id: ${task.id} | title: ${task.title}")
            if (task.description.isNotBlank()) append(" | description: ${task.description}")
            if (task.startTime.isNotBlank()) append(" | fixed_start_time: ${task.startTime}")
            if (task.endTime.isNotBlank()) append(" | fixed_end_time: ${task.endTime}")
            appendLine()
        }
    }

    private fun cleanJson(value: String): String {
        return value.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    /**
     * TEMPORARY DEBUG BUILD: the else branch below surfaces the raw exception
     * message instead of a generic string, so we can see exactly why Gemini,
     * Groq, Cerebras, or OpenRouter are failing. Revert the `else ->` line
     * back to a friendly generic message once the full chain is confirmed working.
     */
    fun friendlyErrorMessage(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("network") || msg.contains("unable to resolve host") ||
                    msg.contains("timeout") ->
                "No internet connection. Check your network and try again."
            msg.contains("blocked") || msg.contains("safety") ->
                "AI couldn't process today's tasks. Try again later."
            msg.contains("quota") || msg.contains("resource_exhausted") ||
                    msg.contains("429") || msg.contains("rate limit") ->
                "AI Daily Planner is busy right now. Please try again in a minute."
            msg.contains("permission") || msg.contains("unauthenticated") ||
                    msg.contains("403") || msg.contains("401") ->
                "AI Daily Planner is temporarily unavailable. Please try again later."
            else ->
                "DEBUG: ${e.message}" // TODO revert to "AI Daily Planner failed. Please try again."
        }
    }
}