package com.example.polar.data.online

import com.example.polar.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// The connection to our online database (Supabase, which is Postgres).
// The address and key come from local.properties, see app/build.gradle.kts.
object Supabase {

    // "by lazy" = only created the first time we use it
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
        }
    }
}
