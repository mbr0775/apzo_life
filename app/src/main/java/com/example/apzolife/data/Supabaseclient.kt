package com.example.apzolife.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://oelfdpfwkdirwmnbkpfq.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9lbGZkcGZ3a2RpcndtbmJrcGZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzUxOTg1OTUsImV4cCI6MjA5MDc3NDU5NX0.-E-WcNvyY-Lve2PsGTYCpm_Oh7Fq7apwgZIm3w2yWGo"
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}