package com.example.writingpractice.data.model

import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.local.db.entity.WeaknessAnalysisEntity
import com.example.writingpractice.data.remote.dto.RecommendedPatternDto
import com.example.writingpractice.data.remote.dto.WeaknessAnalysisDto
import com.example.writingpractice.data.remote.dto.WeaknessPointDto
import com.example.writingpractice.ui.common.Period
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class OverallLevel { BEGINNER, INTERMEDIATE, ADVANCED }

@Serializable
enum class Severity { LOW, MEDIUM, HIGH }

@Serializable
data class WeaknessPoint(
    val errorType: ErrorType,
    val title: String,
    val description: String,
    val examples: List<String> = emptyList(),
    val severity: Severity = Severity.MEDIUM
)

@Serializable
data class RecommendedPattern(
    val pattern: String,
    val exampleSentence: String
)

data class WeaknessAnalysis(
    val id: Long = 0,
    val period: Period,
    val analyzedAt: Long,
    val summary: String,
    val overallLevel: OverallLevel,
    val weaknessPoints: List<WeaknessPoint>,
    val suggestions: List<String>,
    val recommendedPatterns: List<RecommendedPattern>,
    val recommendedLevel: Int,
    val totalCorrections: Int,
    val avgScore: Int?
)

fun WeaknessPointDto.toDomain() = WeaknessPoint(
    errorType = runCatching { ErrorType.valueOf(errorType) }.getOrDefault(ErrorType.GRAMMAR),
    title = title,
    description = description,
    examples = examples,
    severity = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.MEDIUM)
)

fun RecommendedPatternDto.toDomain() = RecommendedPattern(
    pattern = pattern,
    exampleSentence = exampleSentence
)

fun WeaknessAnalysisDto.toDomain(
    period: Period,
    totalCorrections: Int,
    avgScore: Int?,
    analyzedAt: Long = System.currentTimeMillis()
) = WeaknessAnalysis(
    period = period,
    analyzedAt = analyzedAt,
    summary = summary,
    overallLevel = runCatching { OverallLevel.valueOf(overallLevel) }
        .getOrDefault(OverallLevel.INTERMEDIATE),
    weaknessPoints = weaknessPoints.map { it.toDomain() },
    suggestions = improvementSuggestions,
    recommendedPatterns = recommendedPatterns.map { it.toDomain() },
    recommendedLevel = recommendedPracticeLevel.coerceIn(1, 7),
    totalCorrections = totalCorrections,
    avgScore = avgScore
)

fun WeaknessAnalysis.toEntity(json: Json) = WeaknessAnalysisEntity(
    id = id,
    period = period.name,
    analyzedAt = analyzedAt,
    summary = summary,
    overallLevel = overallLevel.name,
    weaknessPointsJson = json.encodeToString<List<WeaknessPoint>>(weaknessPoints),
    suggestionsJson = json.encodeToString<List<String>>(suggestions),
    recommendedPatternsJson = json.encodeToString<List<RecommendedPattern>>(recommendedPatterns),
    recommendedLevel = recommendedLevel,
    totalCorrections = totalCorrections,
    avgScore = avgScore
)

fun WeaknessAnalysisEntity.toDomain(json: Json): WeaknessAnalysis = WeaknessAnalysis(
    id = id,
    period = runCatching { Period.valueOf(period) }.getOrDefault(Period.MONTH),
    analyzedAt = analyzedAt,
    summary = summary,
    overallLevel = runCatching { OverallLevel.valueOf(overallLevel) }
        .getOrDefault(OverallLevel.INTERMEDIATE),
    weaknessPoints = runCatching {
        json.decodeFromString<List<WeaknessPoint>>(weaknessPointsJson)
    }.getOrDefault(emptyList()),
    suggestions = runCatching {
        json.decodeFromString<List<String>>(suggestionsJson)
    }.getOrDefault(emptyList()),
    recommendedPatterns = runCatching {
        json.decodeFromString<List<RecommendedPattern>>(recommendedPatternsJson)
    }.getOrDefault(emptyList()),
    recommendedLevel = recommendedLevel,
    totalCorrections = totalCorrections,
    avgScore = avgScore
)
