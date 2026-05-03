package com.example.writingpractice.data.model

import com.example.writingpractice.data.local.db.entity.CorrectionEntity
import com.example.writingpractice.data.local.db.entity.ErrorType

data class GradingResult(
    val score: Int,
    val overallFeedback: String,
    val corrections: List<Correction>,
    val finalCorrectedVersion: String
)

data class Correction(
    val id: Long = 0,
    val originalSentence: String,
    val correctedSentence: String,
    val explanation: String,
    val errorType: ErrorType,
    val isReviewed: Boolean = false
)

fun CorrectionEntity.toDomain() = Correction(
    id = id,
    originalSentence = originalSentence,
    correctedSentence = correctedSentence,
    explanation = explanation,
    errorType = errorType,
    isReviewed = isReviewed
)

fun Correction.toEntity(userAnswerId: Long, problemId: Long) = CorrectionEntity(
    userAnswerId = userAnswerId,
    problemId = problemId,
    originalSentence = originalSentence,
    correctedSentence = correctedSentence,
    explanation = explanation,
    errorType = errorType
)
