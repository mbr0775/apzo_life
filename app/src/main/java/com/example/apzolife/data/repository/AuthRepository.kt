package com.example.apzolife.data.repository

import com.example.apzolife.data.SessionManager
import com.example.apzolife.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {

    private val auth get() = SupabaseClient.client.auth

    /**
     * Returns true if either:
     *  - Supabase still has an active session in memory, OR
     *  - The app saved a session flag in SharedPreferences (stay logged in)
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null || SessionManager.isLoggedIn
    }

    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean = false
    ): Result<Unit> {
        return runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            if (rememberMe) {
                SessionManager.isLoggedIn = true
                SessionManager.savedEmail = email
            }
        }
    }

    suspend fun signup(
        fullName: String,
        email: String,
        password: String
    ): Result<Unit> {
        return runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
        }
    }

    suspend fun sendReset(email: String): Result<Unit> {
        return runCatching {
            auth.resetPasswordForEmail(email)
        }
    }

    suspend fun logout(): Result<Unit> {
        return runCatching {
            auth.signOut()
            SessionManager.clearSession()
        }
    }

    fun getSavedEmail(): String = SessionManager.savedEmail
}