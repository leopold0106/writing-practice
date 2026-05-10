package com.example.writingpractice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long,
    val answers: List<AnswerBackup>,
    val weaknessAnalyses: List<WeaknessAnalysisBackup> = emptyList(),
    val monthlySnapshots: List<MonthlySnapshotBackup> = emptyList()
)

@Serializable
data class WeaknessAnalysisBackup(
    val period: String,
    val analyzedAt: Long,
    val summary: String,
    val overallLevel: String,
    val weaknessPointsJson: String,
    val suggestionsJson: String,
    val recommendedPatternsJson: String,
    val recommendedLevel: Int,
    val totalCorrections: Int,
    val avgScore: Int? = null
)

@Serializable
data class MonthlySnapshotBackup(
    val yearMonth: String,
    val analyzedAt: Long,
    val comparisonSummary: String,
    val overallTrend: String,
    val errorChangesJson: String,
    val keyImprovementsJson: String,
    val areasToFocusJson: String,
    val currentMonthCorrections: Int,
    val previousMonthCorrections: Int,
    val currentMonthAvgScore: Int? = null,
    val previousMonthAvgScore: Int? = null
)

@Serializable
data class AnswerBackup(
    val problemUuid: String,
    val koreanText: String,
    val referenceAnswer: String? = null,
    val level: Int,
    val topicTag: String? = null,
    val isPrebundled: Boolean,
    val problemCreatedAt: Long,
    val answerText: String,
    val score: Int? = null,
    val gradingStatus: String,
    val overallFeedback: String? = null,
    val finalCorrectedVersion: String? = null,
    val submittedAt: Long,
    val attemptNumber: Int = 1,
    val corrections: List<CorrectionBackup>
)

@Serializable
data class CorrectionBackup(
    val originalSentence: String,
    val correctedSentence: String,
    val explanation: String,
    val errorType: String,
    val isReviewed: Boolean,
    val createdAt: Long
)
