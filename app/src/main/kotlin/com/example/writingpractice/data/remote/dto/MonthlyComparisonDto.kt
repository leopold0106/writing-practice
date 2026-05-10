package com.example.writingpractice.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyComparisonDto(
    @SerialName("comparison_summary") val comparisonSummary: String,
    @SerialName("overall_trend") val overallTrend: String,
    @SerialName("error_type_changes") val errorTypeChanges: List<ErrorTypeChangeDto> = emptyList(),
    @SerialName("key_improvements") val keyImprovements: List<String> = emptyList(),
    @SerialName("areas_to_focus") val areasToFocus: List<String> = emptyList()
)

@Serializable
data class ErrorTypeChangeDto(
    @SerialName("error_type") val errorType: String,
    @SerialName("previous_count") val previousCount: Int,
    @SerialName("current_count") val currentCount: Int,
    val trend: String,
    val insight: String
)
