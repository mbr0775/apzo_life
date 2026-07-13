package com.example.apzolife.data.ai

/**
 * Detects whether a Gemini/Firebase AI failure is a quota/rate-limit
 * problem specifically, as opposed to a real error (bad prompt,
 * network, blocked content). Only quota-type failures should trigger
 * the Groq fallback — other errors should surface normally.
 */
internal fun isQuotaOrRateLimitError(e: Throwable): Boolean {
    val msg = (e.message ?: "").lowercase()
    return msg.contains("quota") ||
            msg.contains("resource_exhausted") ||
            msg.contains("429") ||
            msg.contains("rate limit") ||
            msg.contains("rate_limit")
}