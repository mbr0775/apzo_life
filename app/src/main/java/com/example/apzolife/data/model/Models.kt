package com.example.apzolife.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TaskCategory(val displayName: String, val emoji: String) {
    SOFTWARE_DEVELOPMENT("Software Development", "💻"),
    STUDY("Study", "📖"),
    READING("Reading", "📚"),
    WORK("Work", "💼"),
    PERSONAL("Personal", "🧍"),
    DIET("Diet", "🥗"),
    EXERCISE("Exercise", "🏃"),
    CUSTOM("Custom", "⭐");

    companion object {
        fun fromString(value: String): TaskCategory =
            entries.find { it.name == value } ?: CUSTOM
    }
}

enum class TaskStatus {
    PENDING, DONE, NOT_DONE, IN_PROGRESS
}

@Serializable
data class MainTask(
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    val title: String = "",
    val category: String = TaskCategory.CUSTOM.name,

    @SerialName("date_scheduled")
    val dateScheduled: String = "",

    @SerialName("time_scheduled")
    val timeScheduled: String = "",

    val status: String = TaskStatus.PENDING.name,

    @SerialName("progress_percentage")
    val progressPercentage: Int = 0,

    val notes: String = "",

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("category_data")
    val categoryData: String = "{}"
)

@Serializable
data class SubTask(
    val id: String = "",

    @SerialName("task_id")
    val taskId: String = "",

    @SerialName("user_id")
    val userId: String = "",

    val title: String = "",
    val description: String = "",
    val status: String = TaskStatus.PENDING.name,

    @SerialName("time_scheduled")
    val timeScheduled: String = "",

    @SerialName("progress_percentage")
    val progressPercentage: Int = 0,

    @SerialName("order_index")
    val orderIndex: Int = 0
)

data class AiInsight(
    val message: String,
    val type: InsightType,
    val actionQuestion: String? = null
)

enum class InsightType {
    MOTIVATION, WARNING, ACHIEVEMENT, SUGGESTION
}

data class DailyStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val overallProgress: Int
)

data class CategoryField(
    val key: String,
    val label: String,
    val hint: String,
    val inputType: FieldInputType = FieldInputType.TEXT
)

enum class FieldInputType {
    TEXT,
    NUMBER,
    DATE,
    TIME,
    DATETIME,
    DROPDOWN
}

fun getCategoryFields(category: TaskCategory): List<CategoryField> {
    return when (category) {
        TaskCategory.SOFTWARE_DEVELOPMENT -> listOf(
            CategoryField("module", "Module / Feature", "e.g., Login Screen"),
            CategoryField("deadline", "Deadline", "Pick deadline date & time", FieldInputType.DATETIME),
            CategoryField("techStack", "Tech Stack", "e.g., Kotlin, Compose"),
            CategoryField("priority", "Priority", "High / Medium / Low")
        )

        TaskCategory.STUDY -> listOf(
            CategoryField("subject", "Subject", "e.g., Mathematics"),
            CategoryField("topic", "Topic", "e.g., Calculus"),
            CategoryField("durationMinutes", "Duration (mins)", "e.g., 45", FieldInputType.NUMBER)
        )

        TaskCategory.READING -> listOf(
            CategoryField("bookTitle", "Book Title", "e.g., Atomic Habits"),
            CategoryField("author", "Author", "e.g., James Clear"),
            CategoryField("targetPages", "Pages Target", "e.g., 20", FieldInputType.NUMBER)
        )

        TaskCategory.WORK -> listOf(
            CategoryField("project", "Project", "e.g., Apzo Mobile"),
            CategoryField("priority", "Priority", "High / Medium / Low")
        )

        TaskCategory.PERSONAL -> listOf(
            CategoryField("place", "Place", "e.g., Home / Office"),
            CategoryField("priority", "Priority", "High / Medium / Low")
        )

        TaskCategory.DIET -> listOf(
            CategoryField("mealType", "Meal Type", "e.g., Breakfast, Lunch, Dinner"),
            CategoryField("dietGoal", "Diet Goal", "e.g., Weight loss, Protein intake"),
            CategoryField("calories", "Calories", "e.g., 500", FieldInputType.NUMBER)
        )

        TaskCategory.EXERCISE -> listOf(
            CategoryField("exerciseType", "Exercise Type", "e.g., Running, Gym, Yoga"),
            CategoryField("durationMinutes", "Duration (mins)", "e.g., 30", FieldInputType.NUMBER),
            CategoryField("target", "Target", "e.g., 5km, Chest workout, Stretching")
        )

        TaskCategory.CUSTOM -> listOf(
            CategoryField("customType", "Custom Type", "e.g., Travel, Finance, Habit"),
            CategoryField("customGoal", "Goal / Details", "Enter value"),
            CategoryField("customPriority", "Priority", "High / Medium / Low")
        )
    }
}