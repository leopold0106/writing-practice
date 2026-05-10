package com.example.writingpractice.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaknessAnalysisDto(
    val summary: String,
    @SerialName("overall_level") val overallLevel: String,
    @SerialName("weakness_points") val weaknessPoints: List<WeaknessPointDto> = emptyList(),
    @SerialName("improvement_suggestions") val improvementSuggestions: List<String> = emptyList(),
    @SerialName("recommended_patterns") val recommendedPatterns: List<RecommendedPatternDto> = emptyList(),
    @SerialName("recommended_practice_level") val recommendedPracticeLevel: Int = 3
)

@Serializable
data class WeaknessPointDto(
    @SerialName("error_type") val errorType: String,
    val title: String,
    val description: String,
    val examples: List<String> = emptyList(),
    val severity: String = "MEDIUM"
)

@Serializable
data class RecommendedPatternDto(
    val pattern: String,
    @SerialName("example_sentence") val exampleSentence: String
)
