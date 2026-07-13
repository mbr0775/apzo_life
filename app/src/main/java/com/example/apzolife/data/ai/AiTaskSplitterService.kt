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

@Serializable
data class AiTaskSplitResult(
    @SerialName("task_title")
    val taskTitle: String = "",

    @SerialName("task_description")
    val taskDescription: String = "",

    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 0,

    val subtasks: List<AiSuggestedSubTask> = emptyList()
)

@Serializable
data class AiSuggestedSubTask(
    val title: String = "",
    val description: String = "",

    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 0,

    val children: List<AiSuggestedChildSubTask> = emptyList()
)

@Serializable
data class AiSuggestedChildSubTask(
    val title: String = "",
    val description: String = "",

    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 0
)

object AiTaskSplitterService {

    private const val TAG = "AiTaskSplitter"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val childSubtaskSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "description" to Schema.string(),
            "estimated_minutes" to Schema.integer()
        ),
        optionalProperties = listOf("description", "estimated_minutes")
    )

    private val subtaskSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(),
            "description" to Schema.string(),
            "estimated_minutes" to Schema.integer(),
            "children" to Schema.array(childSubtaskSchema)
        ),
        optionalProperties = listOf("description", "estimated_minutes", "children")
    )

    private val taskSplitSchema = Schema.obj(
        properties = mapOf(
            "task_title" to Schema.string(),
            "task_description" to Schema.string(),
            "estimated_minutes" to Schema.integer(),
            "subtasks" to Schema.array(subtaskSchema)
        ),
        optionalProperties = listOf("task_description", "estimated_minutes")
    )

    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = taskSplitSchema
                maxOutputTokens = 2048
                temperature = 0.6f
            }
        )
    }

    private val groqSystemPrompt = """
        You are Apzo Life AI Task Builder.
        Split the user's main task into practical subtasks and child subtasks.
        The main task can be about ANY subject: software development,
        marketing, fitness, study, business, creative work, household
        projects, or anything else. Do not assume it is a technical topic.
        Keep suggestions simple, realistic, and useful for a daily task tracker app.
        Create 3 to 6 main subtasks.
        Each main subtask can have 0 to 4 child subtasks.
        Respond ONLY with a single valid JSON object, no markdown, no commentary.
        The JSON object MUST match this exact shape:
        {
          "task_title": string,
          "task_description": string,
          "estimated_minutes": integer,
          "subtasks": [
            {
              "title": string,
              "description": string,
              "estimated_minutes": integer,
              "children": [
                { "title": string, "description": string, "estimated_minutes": integer }
              ]
            }
          ]
        }
    """.trimIndent()

    suspend fun splitTask(
        title: String,
        description: String = ""
    ): Result<AiTaskSplitResult> {
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a task title before using AI Task Builder."))
        }

        // 1. Try Gemini first.
        val geminiResult = runCatching { callGemini(cleanTitle, cleanDescription) }
        geminiResult.onSuccess { return Result.success(it) }

        val geminiError = geminiResult.exceptionOrNull()!!
        Log.w(TAG, "Gemini failed: ${geminiError.message}", geminiError)

        if (!isQuotaOrRateLimitError(geminiError)) {
            // Real error (bad prompt, blocked content, network) — don't mask it, surface it.
            return Result.failure(geminiError)
        }

        // 2. Gemini hit its free-tier quota — fall back to Groq.
        Log.i(TAG, "Gemini quota hit, falling back to Groq")
        val groqResult = runCatching { callGroq(cleanTitle, cleanDescription) }
        groqResult.onSuccess { return Result.success(it) }

        val groqError = groqResult.exceptionOrNull()!!
        Log.w(TAG, "Groq failed: ${groqError.message}", groqError)

        if (!isQuotaOrRateLimitError(groqError)) {
            // Real Groq error (not quota) — don't waste further calls, surface it.
            return Result.failure(groqError)
        }

        // 3. Groq also hit its free-tier quota — fall back to Cerebras.
        Log.i(TAG, "Groq quota hit, falling back to Cerebras")
        val cerebrasResult = runCatching { callCerebras(cleanTitle, cleanDescription) }
        cerebrasResult.onSuccess { return Result.success(it) }

        val cerebrasError = cerebrasResult.exceptionOrNull()!!
        Log.w(TAG, "Cerebras failed: ${cerebrasError.message}", cerebrasError)

        if (!isQuotaOrRateLimitError(cerebrasError)) {
            // Real Cerebras error (not quota) — don't waste a fourth call, surface it.
            return Result.failure(cerebrasError)
        }

        // 4. Cerebras ALSO hit its free-tier quota — final fallback to OpenRouter.
        Log.i(TAG, "Cerebras quota hit, falling back to OpenRouter")
        return runCatching { callOpenRouter(cleanTitle, cleanDescription) }
            .onFailure { Log.e(TAG, "OpenRouter fallback also failed", it) }
    }

    private suspend fun callGemini(title: String, description: String): AiTaskSplitResult {
        val prompt = buildGeminiPrompt(title, description)
        val response = geminiModel.generateContent(prompt)

        val finishReason = response.candidates.firstOrNull()?.finishReason
        val responseText = response.text?.trim().orEmpty()
        Log.d(TAG, "Gemini raw response (${responseText.length} chars), finishReason=$finishReason")

        if (responseText.isBlank()) {
            error("AI returned no content (finishReason=${finishReason ?: "unknown"}).")
        }

        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiTaskSplitResult>(cleaned)
    }

    private suspend fun callGroq(title: String, description: String): AiTaskSplitResult {
        val userPrompt = buildString {
            appendLine("Main task title: $title")
            if (description.isNotBlank()) appendLine("Main task description: $description")
        }
        val responseText = GroqClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 2048,
            temperature = 0.6
        )
        Log.d(TAG, "Groq raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiTaskSplitResult>(cleaned)
    }

    private suspend fun callCerebras(title: String, description: String): AiTaskSplitResult {
        val userPrompt = buildString {
            appendLine("Main task title: $title")
            if (description.isNotBlank()) appendLine("Main task description: $description")
        }
        val responseText = CerebrasClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 2048,
            temperature = 0.6
        )
        Log.d(TAG, "Cerebras raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiTaskSplitResult>(cleaned)
    }

    private suspend fun callOpenRouter(title: String, description: String): AiTaskSplitResult {
        val userPrompt = buildString {
            appendLine("Main task title: $title")
            if (description.isNotBlank()) appendLine("Main task description: $description")
        }
        val responseText = OpenRouterClient.generateJson(
            systemPrompt = groqSystemPrompt,
            userPrompt = userPrompt,
            maxTokens = 2048,
            temperature = 0.6
        )
        Log.d(TAG, "OpenRouter raw response (${responseText.length} chars): $responseText")
        val cleaned = cleanJson(responseText)
        return json.decodeFromString<AiTaskSplitResult>(cleaned)
    }

    private fun buildGeminiPrompt(title: String, description: String): String = buildString {
        appendLine("You are Apzo Life AI Task Builder.")
        appendLine("Split the user's main task into practical subtasks and child subtasks.")
        appendLine("The main task can be about ANY subject: software development, ")
        appendLine("marketing, fitness, study, business, creative work, household ")
        appendLine("projects, or anything else. Do not assume it is a technical topic.")
        appendLine("Keep suggestions simple, realistic, and useful for a daily task tracker app.")
        appendLine("Create 3 to 6 main subtasks.")
        appendLine("Each main subtask can have 0 to 4 child subtasks.")
        appendLine("Do not create too many items.")
        appendLine("Do not include markdown.")
        appendLine("Return only JSON matching the provided schema.")
        appendLine()
        appendLine("Main task title: $title")
        if (description.isNotBlank()) appendLine("Main task description: $description")
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
                "AI couldn't process that title. Try rephrasing it."
            msg.contains("quota") || msg.contains("resource_exhausted") ||
                    msg.contains("429") || msg.contains("rate limit") ->
                "AI Task Builder is busy right now. Please try again in a moment."
            msg.contains("permission") || msg.contains("unauthenticated") ||
                    msg.contains("403") || msg.contains("401") ->
                "AI Task Builder is temporarily unavailable. Please try again later."
            else ->
                "DEBUG: ${e.message}" // TODO revert to "AI Task Builder failed. Please try again."
        }
    }
}