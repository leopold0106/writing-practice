package com.example.writingpractice.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaudeResponse(
    val id: String,
    val content: List<ContentBlock>,
    val usage: Usage
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String = ""
)

@Serializable
data class Usage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

@Serializable
data class GradingResultDto(
    val score: Int,
    @SerialName("overall_feedback") val overallFeedback: String,
    val corrections: List<CorrectionDto>,
    @SerialName("final_corrected_version") val finalCorrectedVersion: String
)

@Serializable
data class CorrectionDto(
    @SerialName("original_sentence") val originalSentence: String,
    @SerialName("corrected_sentence") val correctedSentence: String,
    val explanation: String,
    @SerialName("error_type") val errorType: String
)

@Serializable
data class GeneratedProblemDto(
    @SerialName("korean_text") val koreanText: String,
    @SerialName("reference_answer") val referenceAnswer: String,
    @SerialName("topic_tag") val topicTag: String = ""
)
