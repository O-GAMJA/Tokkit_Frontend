package com.example.tokkit.data.remote.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ReviewStageItem(
    val uuid: String,
    val stage: String
)

data class ReviewDetailResponse(
    val noteId: String,
    val noteTitle: String,
    val currentStage: String,
    val reviewCurvePoints: List<ReviewCurvePoint>,
    val reviewStats: List<ReviewStat>
)

data class ReviewCurvePoint(
    val reviewedAt: String,
    val stageAfterReview: String
)

data class ReviewStat(
    val type: String, // QUIZ, TALK 등
    val score: Int,
    val correctCount: Int,
    val totalCount: Int,
    val reviewedAt: String
)

data class ConversationReviewRequest(
    val content: String
)

data class ReviewResponse(
    val reviewResult: String,
    val newStage: String,
    val nextReviewAt: String
)

@Parcelize
data class QuizItem(
    val noteId: String,
    val quizId: Long,
    val difficulty: String,
    val question: String,
    val answer: Int,
    val choices: List<String>,
    val explanation: String
) : Parcelable

data class QuizListResponse(
    val quizzes: List<QuizItem>,
    val paginationInfo: PaginationInfo
)

data class QuizReviewRequest(
    val score: Int,
    val correctCount: Int
)