package com.example.writingpractice.data.repository

import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.MonthlySnapshotDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.MonthlySnapshot
import com.example.writingpractice.data.model.toDomain
import com.example.writingpractice.data.model.toEntity
import com.example.writingpractice.data.remote.ClaudeApiClient
import com.example.writingpractice.data.remote.MonthlyComparisonInput
import com.example.writingpractice.data.remote.SampleCorrection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyAnalysisRepository @Inject constructor(
    private val correctionDao: CorrectionDao,
    private val userAnswerDao: UserAnswerDao,
    private val monthlySnapshotDao: MonthlySnapshotDao,
    private val claudeApiClient: ClaudeApiClient,
    private val json: Json
) {
    fun observeAll(): Flow<List<MonthlySnapshot>> =
        monthlySnapshotDao.observeAll().map { list -> list.map { it.toDomain(json) } }

    suspend fun analyzeMonthly(currentYM: String): Result<MonthlySnapshot> {
        val previousYM = YearMonth.parse(currentYM).minusMonths(1).toString()

        val currStart = currentYM.toMonthStartMs()
        val currEnd = currentYM.toMonthEndMs()
        val prevStart = previousYM.toMonthStartMs()
        val prevEnd = previousYM.toMonthEndMs()

        val prevTotal = correctionDao.countCorrectionsInRange(prevStart, prevEnd)
        if (prevTotal == 0) {
            return Result.failure(IllegalStateException("지난 달 데이터가 없어 비교할 수 없습니다."))
        }

        val currTotal = correctionDao.countCorrectionsInRange(currStart, currEnd)
        val prevSamples = correctionDao.getCorrectionsInRange(prevStart, prevEnd, 15)
        val currSamples = correctionDao.getCorrectionsInRange(currStart, currEnd, 15)
        val prevAvgScore = userAnswerDao.avgScoreInRange(prevStart, prevEnd)?.toInt()
        val currAvgScore = userAnswerDao.avgScoreInRange(currStart, currEnd)?.toInt()

        val prevCounts = ErrorType.entries.associate { type ->
            type.name to prevSamples.count { it.errorType == type }
        }
        val currCounts = ErrorType.entries.associate { type ->
            type.name to currSamples.count { it.errorType == type }
        }

        val input = MonthlyComparisonInput(
            currentYearMonth = currentYM,
            previousYearMonth = previousYM,
            currentTotalCorrections = currTotal,
            previousTotalCorrections = prevTotal,
            currentCounts = currCounts,
            previousCounts = prevCounts,
            currentSamples = currSamples.map {
                SampleCorrection(it.originalSentence, it.correctedSentence, it.explanation, it.errorType.name)
            },
            previousSamples = prevSamples.map {
                SampleCorrection(it.originalSentence, it.correctedSentence, it.explanation, it.errorType.name)
            },
            currentAvgScore = currAvgScore,
            previousAvgScore = prevAvgScore
        )

        return claudeApiClient.compareMonthlyPatterns(input).mapCatching { dto ->
            val domain = dto.toDomain(
                yearMonth = currentYM,
                analyzedAt = System.currentTimeMillis(),
                currentMonthCorrections = currTotal,
                previousMonthCorrections = prevTotal,
                currentMonthAvgScore = currAvgScore,
                previousMonthAvgScore = prevAvgScore
            )
            monthlySnapshotDao.insert(domain.toEntity(json))
            domain
        }
    }

    private fun String.toMonthStartMs(): Long =
        YearMonth.parse(this).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun String.toMonthEndMs(): Long =
        YearMonth.parse(this).plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
