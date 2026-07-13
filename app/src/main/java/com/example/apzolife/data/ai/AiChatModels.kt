package com.example.apzolife.data.ai

/** One turn in the Ask Apzo AI conversation, kept in memory in the ViewModel. */
data class AiChatTurn(
    val role: String, // "user" or "assistant"
    val text: String
)