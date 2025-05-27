package com.example.tokkit.data.remote.model

data class LoginRequest(
    val email: String,
    val password: String,
    val fcmToken: String
)