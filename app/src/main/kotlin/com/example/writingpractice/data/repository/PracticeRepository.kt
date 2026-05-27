package com.example.writingpractice.data.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.example.writingpractice.widget.StreakWidgetProvider
import com.example.writingpractice.worker.GradeAnswerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
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
        notifyStreakWidget()
    }

    private fun notifyStreakWidget() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, StreakWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            val intent = Intent(context, StreakWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
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

    fun observeCurrentStreak(): Flow<Int> =
        progressDao.observeAllActiveDates().map { dates -> computeStreak(dates) }

    private fun computeStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val today = LocalDate.now()
        val sorted = dates.map { LocalDate.parse(it) }.sortedDescending()
        val mostRecent = sorted.first()
        // If the most recent active day is older than yesterday, streak is broken
        if (mostRecent.isBefore(today.minusDays(1))) return 0
        var streak = 0
        var expected = if (mostRecent == today) today else today.minusDays(1)
        for (date in sorted) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date.isBefore(expected)) {
                break
            }
        }
        return streak
    }
}
