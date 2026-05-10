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
import com.example.writingpractice.di.ApplicationScope
import com.example.writingpractice.ui.common.Period
import com.example.writingpractice.ui.common.sinceMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeaknessAnalysisRepository @Inject constructor(
    private val correctionDao: CorrectionDao,
    private val userAnswerDao: UserAnswerDao,
    private val weaknessAnalysisDao: WeaknessAnalysisDao,
    private val claudeApiClient: ClaudeApiClient,
    private val json: Json,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun observeLatestForPeriod(period: Period): Flow<WeaknessAnalysis?> =
        weaknessAnalysisDao.observeLatestByPeriod(period.name).map { entity ->
            entity?.toDomain(json)
        }

    fun observeHistory(): Flow<List<WeaknessAnalysis>> =
        weaknessAnalysisDao.observeAll().map { list -> list.map { it.toDomain(json) } }

    suspend fun countCorrectionsForPeriod(period: Period): Int =
        correctionDao.countCorrectionsAfter(period.sinceMs)

    fun dismissError() {
        _lastError.value = null
    }

    /**
     * Fire-and-forget trigger. Analysis runs in the application scope so it survives
     * ViewModel destruction (navigating away and back). Concurrent calls are no-ops
     * — the atomic compareAndSet on _isAnalyzing serializes entry.
     */
    fun triggerAnalyze(period: Period) {
        if (!_isAnalyzing.compareAndSet(expect = false, update = true)) return
        applicationScope.launch {
            _lastError.value = null
            try {
                analyze(period).onFailure { e ->
                    _lastError.value = e.message ?: "알 수 없는 오류"
                }
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

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
