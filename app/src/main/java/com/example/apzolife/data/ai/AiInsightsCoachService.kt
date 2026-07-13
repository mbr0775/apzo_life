package com.example.apzolife.data.ai

import android.util.Log
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.TaskStatus
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class AiInsightsResult(
    @SerialName("weekly_summary")
    val weeklySummary: String = "",

    @SerialName("missed_reason")
    val missedReason: String = "",

    @SerialName("productivity_tip")
    val productivityTip: String = "",

    @SerialName("next_best_action")
    val nextBestAction: String = "",

    @SerialName("tomorrow_suggestion")
    val tomorrowSuggestion: String = ""
)

/**
 * AI Insights Coach — reads the user's last 7 days of task activity and turns
 * raw completion stats into a short, human-readable coaching summary instead
 * of just a percentage. Same 4-tier fallback chain as the other AI features:
 * Gemini → Groq → Cerebras → OpenRouter, each hop triggered only on a
 * quota/rate-limit error.
 */
object AiInsightsCoachService {

    private const val TAG = "AiInsightsCoach"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val insightsSchema = Schema.obj(
        properties = mapOf(
            "weekly_summary" to Schema.string(),
            "missed_reason" to Schema.string(),
            "productivity_tip" to Schema.string(),
            "next_best_action" to Schema.string(),
            "tomorrow_suggestion" to Schema.string()
        )
    )

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightsSchema
                maxOutputTokens = 1024
                temperature = 0.5f
            }
        )
    }

    private val systemPrompt = """
        You are Apzo Life AI Coach, a supportive but honest productivity coach
        reading a user's task-completion history for the last 7 days.
        Look at which tasks were completed vs missed, and infer patterns from
        the task titles (subject area, time of day if given, task size).
        Rules:
        - weekly_summary: 1-2 plain sentences stating how many tasks were
          completed out of the total, in a natural, encouraging tone.
        - missed_reason: 1 short sentence naming the most likely pattern behind
          missed tasks (e.g. a topic area, task size, or timing issue). If
          nothing was missed, say something positive instead.
        - productivity_tip: one concrete, specific tip based on the pattern
          found, not generic advice.
        - next_best_action: one specific action the user should take right now
          today, phrased as an instruction.
        - tomorrow_suggestion: one specific suggestion for how to structure
          tomorrow (e.g. what to schedule earlier, what to break down smaller).
        Keep every field short — one or two sentences max. Be direct and
        practical, not vague or generic. Do not use markdown formatting.
        Respond ONLY with a single valid JSON object, no commentary.
        The JSON object MUST match this exact shape:
        {
          "weekly_summary": string,
          "missed_reason": string,
          "productivity_tip": string,
          "next_best_action": string,
          "tomorrow_suggestion": string
        }
    """.trimIndent()

    suspend fun generateInsights(allTasks: List<MainTask>): Result<AiInsightsResult> {
        val recentTasks = filterLastSevenDays(allTasks)

        if (recentTasks.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Not enough recent task data yet to generate insights.")
            )
        }

        // 1. Try Gemini first.
        val geminiResult = runCatching { callGemini(recentTasks) }
        geminiResult.onSuccess { return Result.success(it) }

        val geminiError = geminiResult.exceptionOrNull()!!
        Log.w(TAG, "Gemini failed: ${geminiError.message}", geminiError)

        if (!isQuotaOrRateLimitError(geminiError)) {
            return Result.failure(geminiError)
        }

        // 2. Gemini hit its free-tier quota — fall back to Groq.
        Log.i(TAG, "Gemini quota hit, falling back to Groq")
        val groqResult = runCatching { callGroq(recentTasks) }
        groqResult.onSuccess { return Result.success(it) }

        val groqError = groqResult.exceptionOrNull()!!
        Log.w(TAG, "Groq failed: ${groqError.message}", groqError)

        if (!isQuotaOrRateLimitError(groqError)) {
            return Result.failure(groqError)
        }

        // 3. Groq also hit its free-tier quota — fall back to Cerebras.
        Log.i(TAG, "Groq quota hit, falling back to Cerebras")
        val cerebrasResult = runCatching { callCerebras(recentTasks) }
        cerebrasResult.onSuccess { return Result.success(it) }

        val cerebrasError = cerebrasResult.exceptionOrNull()!!
        Log.w(TAG, "Cerebras failed: ${cerebrasError.message}", cerebrasError)

        if (!isQuotaOrRateLimitError(cerebrasError)) {
            return Result.failure(cerebrasError)
        }

        // 4. Cerebras ALSO hit its free-tier quota — final fallback to OpenRouter.
        Log.i(TAG, "Cerebras quota hit, falling back to OpenRouter")
        return runCatching { callOpenRouter(recentTasks) }
            .onFailure { Log.e(TAG, "OpenRouter fallback also failed", it) }
    }

    private suspend fun callGemini(tasks: List<MainTask>): AiInsightsResult {
        val prompt = systemPrompt + "\n\n" + buildActivityPrompt(tasks)
        val response = geminiModel.generateContent(prompt)

        val finishReason = response.candidates.firstOrNull()?.finishReason
        val responseText = response.text?.trim().orEmpty()
        Log.d(TAG, "Gemini raw response (${responseText.length} chars), finishReason=$finishReason")

        if (responseText.isBlank()) {
            error("AI returned no insights (finishReason=${finishReason ?: "unknown"}).")
        }

        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiInsightsResult>(cleaned)
    }

    private suspend fun callGroq(tasks: List<MainTask>): AiInsightsResult {
        val responseText = GroqClient.generateJson(
            systemPrompt = systemPrompt,
            userPrompt = buildActivityPrompt(tasks),
            maxTokens = 1024,
            temperature = 0.5
        )
        Log.d(TAG, "Groq raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiInsightsResult>(cleaned)
    }

    private suspend fun callCerebras(tasks: List<MainTask>): AiInsightsResult {
        val responseText = CerebrasClient.generateJson(
            systemPrompt = systemPrompt,
            userPrompt = buildActivityPrompt(tasks),
            maxTokens = 1024,
            temperature = 0.5
        )
        Log.d(TAG, "Cerebras raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiInsightsResult>(cleaned)
    }

    private suspend fun callOpenRouter(tasks: List<MainTask>): AiInsightsResult {
        val responseText = OpenRouterClient.generateJson(
            systemPrompt = systemPrompt,
            userPrompt = buildActivityPrompt(tasks),
            maxTokens = 1024,
            temperature = 0.5
        )
        Log.d(TAG, "OpenRouter raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiInsightsResult>(cleaned)
    }

    private fun filterLastSevenDays(allTasks: List<MainTask>): List<MainTask> {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6) // inclusive 7-day window
        return allTasks.filter { task ->
            try {
                val date = LocalDate.parse(task.startDate)
                !date.isBefore(weekAgo) && !date.isAfter(today)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun buildActivityPrompt(tasks: List<MainTask>): String = buildString {
        val done = tasks.filter { it.status == TaskStatus.DONE.name }
        val missed = tasks.filter { it.status == TaskStatus.NOT_DONE.name }
        val pending = tasks.filter { it.status == TaskStatus.PENDING.name }
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

        appendLine("Today is $today.")
        appendLine("User's task activity for the last 7 days:")
        appendLine("Total tasks: ${tasks.size}")
        appendLine("Completed: ${done.size}")
        appendLine("Missed (marked not done): ${missed.size}")
        appendLine("Still pending: ${pending.size}")
        appendLine()

        if (done.isNotEmpty()) {
            appendLine("Completed task titles:")
            done.take(15).forEach { appendLine("- ${it.title}") }
            appendLine()
        }
        if (missed.isNotEmpty()) {
            appendLine("Missed task titles:")
            missed.take(15).forEach { appendLine("- ${it.title}") }
            appendLine()
        }
        if (pending.isNotEmpty()) {
            appendLine("Still-pending task titles:")
            pending.take(10).forEach { appendLine("- ${it.title}") }
        }
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
            msg.contains("not enough recent task data") ->
                "Complete a few tasks first so AI Coach has something to work with."
            msg.contains("network") || msg.contains("unable to resolve host") ||
                    msg.contains("timeout") ->
                "No internet connection. Check your network and try again."
            msg.contains("blocked") || msg.contains("safety") ->
                "AI Coach couldn't process your task history. Try again later."
            msg.contains("quota") || msg.contains("resource_exhausted") ||
                    msg.contains("429") || msg.contains("rate limit") ->
                "AI Coach is busy right now. Please try again in a moment."
            msg.contains("permission") || msg.contains("unauthenticated") ||
                    msg.contains("403") || msg.contains("401") ->
                "AI Coach is temporarily unavailable. Please try again later."
            else ->
                "DEBUG: ${e.message}" // TODO revert to "AI Coach couldn't generate insights. Please try again."
        }
    }
}