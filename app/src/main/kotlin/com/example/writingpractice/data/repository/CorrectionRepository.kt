package com.example.writingpractice.data.repository

import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
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
    private val problemDao: ProblemDao
) {
    fun observeUnreviewedCount(): Flow<Int> = correctionDao.observeUnreviewedCount()

    fun observeNotebookEntries(): Flow<List<NotebookEntry>> =
        correctionDao.observeDistinctProblemIds().flatMapLatest { problemIds ->
            if (problemIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val flows = problemIds.map { pid ->
                correctionDao.observeForProblem(pid).map { corrections ->
                    pid to corrections.map { it.toDomain() }
                }
            }
            combine(flows) { pairs ->
                val entries = mutableListOf<NotebookEntry>()
                for ((pid, corrections) in pairs) {
                    if (corrections.isEmpty()) continue
                    val problem = problemDao.getById(pid) ?: continue
                    entries.add(
                        NotebookEntry(
                            problemId = pid,
                            koreanText = problem.koreanText,
                            level = problem.level,
                            corrections = corrections
                        )
                    )
                }
                entries
            }
        }

    fun observeCorrectionsForProblem(problemId: Long): Flow<List<Correction>> =
        correctionDao.observeForProblem(problemId).map { list -> list.map { it.toDomain() } }

    suspend fun markProblemReviewed(problemId: Long) =
        correctionDao.markReviewed(problemId)

    suspend fun getMostCommonErrorTypes(limit: Int): List<String> =
        correctionDao.getMostCommonErrorTypes(limit)
}
