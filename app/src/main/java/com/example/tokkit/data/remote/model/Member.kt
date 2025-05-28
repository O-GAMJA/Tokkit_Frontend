package com.example.tokkit.data.remote.model

data class MemberProfileResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val notificationAgree: Boolean
)