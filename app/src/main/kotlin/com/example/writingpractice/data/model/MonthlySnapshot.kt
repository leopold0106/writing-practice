package com.example.writingpractice.data.model

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.local.db.entity.MonthlySnapshotEntity
import com.example.writingpractice.data.remote.dto.MonthlyComparisonDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class MonthlyTrend { IMPROVING, DECLINING, STABLE, MIXED }

@Serializable
enum class ErrorTrend { IMPROVED, WORSENED, STABLE }

@Serializable
data class ErrorTypeChange(
    val errorType: ErrorType,
    val previousCount: Int,
    val currentCount: Int,
    val trend: ErrorTrend,
    val insight: String
)

data class MonthlySnapshot(
    val yearMonth: String,
    val analyzedAt: Long,
    val comparisonSummary: String,
    val overallTrend: MonthlyTrend,
    val errorChanges: List<ErrorTypeChange>,
    val keyImprovements: List<String>,
    val areasToFocus: List<String>,
    val currentMonthCorrections: Int,
    val previousMonthCorrections: Int,
    val currentMonthAvgScore: Int?,
    val previousMonthAvgScore: Int?
)

fun MonthlyComparisonDto.toDomain(
    yearMonth: String,
    analyzedAt: Long,
    currentMonthCorrections: Int,
    previousMonthCorrections: Int,
    currentMonthAvgScore: Int?,
    previousMonthAvgScore: Int?
): MonthlySnapshot = MonthlySnapshot(
    yearMonth = yearMonth,
    analyzedAt = analyzedAt,
    comparisonSummary = comparisonSummary,
    overallTrend = runCatching { MonthlyTrend.valueOf(overallTrend) }.getOrDefault(MonthlyTrend.STABLE),
    errorChanges = errorTypeChanges.map { c ->
        ErrorTypeChange(
            errorType = runCatching { ErrorType.valueOf(c.errorType) }.getOrDefault(ErrorType.GRAMMAR),
            previousCount = c.previousCount,
            currentCount = c.currentCount,
            trend = runCatching { ErrorTrend.valueOf(c.trend) }.getOrDefault(ErrorTrend.STABLE),
            insight = c.insight
        )
    },
    keyImprovements = keyImprovements,
    areasToFocus = areasToFocus,
    currentMonthCorrections = currentMonthCorrections,
    previousMonthCorrections = previousMonthCorrections,
    currentMonthAvgScore = currentMonthAvgScore,
    previousMonthAvgScore = previousMonthAvgScore
)

fun MonthlySnapshot.toEntity(json: Json): MonthlySnapshotEntity = MonthlySnapshotEntity(
    yearMonth = yearMonth,
    analyzedAt = analyzedAt,
    comparisonSummary = comparisonSummary,
    overallTrend = overallTrend.name,
    errorChangesJson = json.encodeToString<List<ErrorTypeChange>>(errorChanges),
    keyImprovementsJson = json.encodeToString<List<String>>(keyImprovements),
    areasToFocusJson = json.encodeToString<List<String>>(areasToFocus),
    currentMonthCorrections = currentMonthCorrections,
    previousMonthCorrections = previousMonthCorrections,
    currentMonthAvgScore = currentMonthAvgScore,
    previousMonthAvgScore = previousMonthAvgScore
)

fun MonthlySnapshotEntity.toDomain(json: Json): MonthlySnapshot = MonthlySnapshot(
    yearMonth = yearMonth,
    analyzedAt = analyzedAt,
    comparisonSummary = comparisonSummary,
    overallTrend = runCatching { MonthlyTrend.valueOf(overallTrend) }.getOrDefault(MonthlyTrend.STABLE),
    errorChanges = runCatching {
        json.decodeFromString<List<ErrorTypeChange>>(errorChangesJson)
    }.getOrDefault(emptyList()),
    keyImprovements = runCatching {
        json.decodeFromString<List<String>>(keyImprovementsJson)
    }.getOrDefault(emptyList()),
    areasToFocus = runCatching {
        json.decodeFromString<List<String>>(areasToFocusJson)
    }.getOrDefault(emptyList()),
    currentMonthCorrections = currentMonthCorrections,
    previousMonthCorrections = previousMonthCorrections,
    currentMonthAvgScore = currentMonthAvgScore,
    previousMonthAvgScore = previousMonthAvgScore
)
