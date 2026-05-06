package com.example.writingpractice.data.repository

import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.dao.WeaknessAnalysisDao
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.WeaknessAnalysis
import com.example.writingpractice.data.model.toDomain
import com.example.writingpractice.data.model.toEntity
import com.example.writingpractice.data.remote.ClaudeApiClient
import com.example.writingpractice.data.remote.SampleCorrection
import com.example.writingpractice.data.remote.WeaknessAnalysisInput
import com.example.writingpractice.ui.common.Period
import com.example.writingpractice.ui.common.sinceMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeaknessAnalysisRepository @Inject constructor(
    private val correctionDao: CorrectionDao,
    private val userAnswerDao: UserAnswerDao,
    private val weaknessAnalysisDao: WeaknessAnalysisDao,
    private val claudeApiClient: ClaudeApiClient,
    private val json: Json
) {

    fun observeLatestForPeriod(period: Period): Flow<WeaknessAnalysis?> =
        weaknessAnalysisDao.observeLatestByPeriod(period.name).map { entity ->
            entity?.toDomain(json)
        }

    fun observeHistory(): Flow<List<WeaknessAnalysis>> =
        weaknessAnalysisDao.observeAll().map { list -> list.map { it.toDomain(json) } }

    suspend fun countCorrectionsForPeriod(period: Period): Int =
        correctionDao.countCorrectionsAfter(period.sinceMs)

    suspend fun analyze(period: Period): Result<WeaknessAnalysis> {
        val sinceMs = period.sinceMs
        val total = correctionDao.countCorrectionsAfter(sinceMs)
        if (total == 0) {
            return Result.failure(IllegalStateException("이 기간에 분석할 오답이 없습니다."))
        }

        val sample = correctionDao.getRecentCorrections(sinceMs, limit = 30)
        val countsByType: Map<String, Int> = ErrorType.entries.associate { type ->
            type.name to sample.count { it.errorType == type }
        }
        val avgScore = userAnswerDao.avgScoreAfter(sinceMs)?.toInt()

        val input = WeaknessAnalysisInput(
            periodLabel = period.label,
            totalCorrections = total,
            avgScore = avgScore,
            errorCountsByType = countsByType,
            sampleCorrections = sample.map {
                SampleCorrection(
                    original = it.originalSentence,
                    corrected = it.correctedSentence,
                    explanation = it.explanation,
                    errorType = it.errorType.name
                )
            }
        )

        return claudeApiClient.analyzeWeaknesses(input).mapCatching { dto ->
            val domain = dto.toDomain(period, totalCorrections = total, avgScore = avgScore)
            val newId = weaknessAnalysisDao.insert(domain.toEntity(json))
            domain.copy(id = newId)
        }
    }
}
