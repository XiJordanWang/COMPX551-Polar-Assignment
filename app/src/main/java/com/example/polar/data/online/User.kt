package com.example.polar.data.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One row in the online "users" table (Supabase).
// @Serializable lets the Supabase library turn it into JSON and back.
// @SerialName is the column name in the database (Postgres uses snake_case).
@Serializable
data class User(
    val username: String,                                // primary key
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    // Never the real password, only the salted hash from hashPassword()
    @SerialName("password_hash") val passwordHash: String,
    val sharing: Boolean = false,
    val streak: Int = 0
)
