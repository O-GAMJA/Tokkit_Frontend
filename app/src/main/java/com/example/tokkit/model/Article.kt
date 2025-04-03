package com.example.tokkit.model

data class Article(
    val title: String,
    val content: String,
    val date: String,
    val imageResId: Int,
    val stage: Int = 0 // 단계 정보, 0-> 전체 1-5 -> 나머지
)