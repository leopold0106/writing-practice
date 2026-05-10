package com.example.writingpractice.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.local.db.dao.ProgressDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.DailyProgressEntity
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.GradingResult
import com.example.writingpractice.data.model.toDomain
import com.example.writingpractice.data.model.toEntity
import com.example.writingpractice.data.remote.ClaudeApiClient
import com.example.writingpractice.util.DateTimeUtil
import com.example.writingpractice.worker.GradeAnswerWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepository @Inject constructor(
    private val userAnswerDao: UserAnswerDao,
    private val correctionDao: CorrectionDao,
    private val progressDao: ProgressDao,
    private val problemDao: ProblemDao,
    private val claudeApiClient: ClaudeApiClient,
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository
) {
    // In-memory draft storage; survives navigation within the same app session
    private val drafts = mutableMapOf<Long, String>()
    fun getDraft(problemId: Long): String = drafts[problemId] ?: ""
    fun saveDraft(problemId: Long, text: String) { drafts[problemId] = text }
    fun clearDraft(problemId: Long) { drafts.remove(problemId) }

    suspend fun submitAnswer(problemId: Long, answerText: String): Long {
        val existing = userAnswerDao.observeForProblem(problemId).first()
        val attemptNumber = existing.size + 1
        val entity = UserAnswerEntity(
            problemId = problemId,
            answerText = answerText,
            submittedAt = System.currentTimeMillis(),
            gradingStatus = GradingStatus.PENDING,
            attemptNumber = attemptNumber
        )
        val answerId = userAnswerDao.insert(entity)
        // Try direct grading immediately; fall back to WorkManager on failure (offline/error)
        val result = gradeAnswer(answerId)
        if (result.isFailure) {
            enqueueGrading(answerId)
        }
        return answerId
    }

    private fun enqueueGrading(answerId: Long) {
        val request = OneTimeWorkRequestBuilder<GradeAnswerWorker>()
            .setInputData(workDataOf(GradeAnswerWorker.KEY_ANSWER_ID to answerId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "grade_answer_$answerId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun gradeAnswer(answerId: Long): Result<GradingResult> {
        val answer = userAnswerDao.getById(answerId)
            ?: return Result.failure(IllegalStateException("Answer not found"))
        val problem = problemDao.getById(answer.problemId)
            ?: return Result.failure(IllegalStateException("Problem not found"))

        return claudeApiClient.gradeAnswer(problem.koreanText, answer.answerText)
            .onSuccess { result ->
                userAnswerDao.update(
                    answer.copy(
                        gradingStatus = GradingStatus.GRADED,
                        score = result.score,
                        overallFeedback = result.overallFeedback,
                        finalCorrectedVersion = result.finalCorrectedVersion
                    )
                )
                if (result.corrections.isNotEmpty()) {
                    correctionDao.insertAll(
                        result.corrections.map { c ->
                            c.toEntity(userAnswerId = answerId, problemId = problem.id)
                        }
                    )
                }
                updateDailyProgress()
            }
    }

    private suspend fun updateDailyProgress() {
        val today = DateTimeUtil.todayIso()
        val goal = settingsRepository.dailyGoal.first()
        val current = progressDao.getForDate(today)
        val solved = (current?.problemsSolved ?: 0) + 1
        progressDao.upsert(
            DailyProgressEntity(
                date = today,
                problemsSolved = solved,
                dailyGoal = goal,
                levelBreakdown = current?.levelBreakdown ?: "{}"
            )
        )
    }

    suspend fun getGradingStatus(answerId: Long): GradingStatus? =
        userAnswerDao.getById(answerId)?.gradingStatus

    fun observeGradingStatus(answerId: Long): Flow<GradingStatus?> =
        userAnswerDao.observeById(answerId).map { it?.gradingStatus }

    fun observeAnswer(answerId: Long) = userAnswerDao.observeById(answerId)

    fun observeCorrectionsForAnswer(answerId: Long): Flow<List<Correction>> =
        correctionDao.observeForAnswer(answerId).map { list -> list.map { it.toDomain() } }

    fun observeTodaySolvedCount(): Flow<Int> =
        userAnswerDao.observeCountForDate(DateTimeUtil.todayIso())
}
