package com.akproleter.mobile.data.remote.models

data class UserDto(
    val id: String,
    val name: String?,
    val email: String?,
    val role: String,
    val token: String? = null // For JWT
)

enum class UserRole {
    ADMIN, ATHLETE, USER
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val user: UserDto,
    val token: String
)
