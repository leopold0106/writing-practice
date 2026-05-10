package com.example.writingpractice.data.repository

import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.NotebookEntry
import com.example.writingpractice.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrectionRepository @Inject constructor(
    private val correctionDao: CorrectionDao,
    private val problemDao: ProblemDao,
    private val userAnswerDao: UserAnswerDao
) {
    fun observeUnreviewedCount(): Flow<Int> = correctionDao.observeUnreviewedCount()

    fun observeNotebookEntries(): Flow<List<NotebookEntry>> =
        correctionDao.observeDistinctProblemIds().flatMapLatest { problemIds ->
            if (problemIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = problemIds.map { pid ->
                correctionDao.observeForProblem(pid).map { entities -> pid to entities }
            }
            combine(flows) { pairs ->
                val entries = mutableListOf<NotebookEntry>()
                for ((pid, entities) in pairs) {
                    if (entities.isEmpty()) continue
                    val problem = problemDao.getById(pid) ?: continue
                    val latestScore = userAnswerDao.getLatestScoreForProblem(pid)
                    entries.add(
                        NotebookEntry(
                            problemId = pid,
                            koreanText = problem.koreanText,
                            level = problem.level,
                            corrections = entities.map { it.toDomain() },
                            latestAnsweredAt = entities.maxOf { it.createdAt },
                            latestScore = latestScore
                        )
                    )
                }
                entries
            }
        }

    fun observeCorrectionsForProblem(problemId: Long): Flow<List<Correction>> =
        correctionDao.observeForProblem(problemId).map { list -> list.map { it.toDomain() } }

    fun observeErrorCounts(sinceMs: Long): Flow<Map<ErrorType, Int>> =
        correctionDao.observeCorrectionsAfter(sinceMs).map { corrections ->
            ErrorType.entries.associateWith { type -> corrections.count { it.errorType == type } }
        }

    suspend fun markProblemReviewed(problemId: Long) =
        correctionDao.markReviewed(problemId)

    suspend fun getMostCommonErrorTypes(limit: Int): List<String> =
        correctionDao.getMostCommonErrorTypes(limit)
}
