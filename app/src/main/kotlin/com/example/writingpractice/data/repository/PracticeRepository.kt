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
import com.example.writingpractice.data.model.toDomain
import com.example.writingpractice.data.model.toEntity
import com.example.writingpractice.data.remote.ApiFailure
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
        // Register the fallback worker *before* the inline attempt. If this coroutine is cancelled
        // (the user leaves the screen) or the process dies mid-call, the answer would otherwise be
        // stranded at PENDING with nothing scheduled to ever finish it. The delay keeps the worker
        // from racing the inline call; gradeAnswer() is idempotent in any case.
        enqueueGrading(answerId, delaySeconds = INLINE_GRADING_GRACE_SECONDS)
        val result = gradeAnswer(answerId)
        // Success and permanent failure are both terminal; only a retryable failure needs the worker.
        if ((result.exceptionOrNull() as? ApiFailure)?.retryable != true) {
            workManager.cancelUniqueWork(workName(answerId))
        }
        return answerId
    }

    private fun workName(answerId: Long) = "grade_answer_$answerId"

    private fun enqueueGrading(
        answerId: Long,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        delaySeconds: Long = 0
    ) {
        val builder = OneTimeWorkRequestBuilder<GradeAnswerWorker>()
            .setInputData(workDataOf(GradeAnswerWorker.KEY_ANSWER_ID to answerId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        if (delaySeconds > 0) {
            builder.setInitialDelay(delaySeconds, TimeUnit.SECONDS)
        }
        workManager.enqueueUniqueWork(workName(answerId), policy, builder.build())
    }

    /**
     * Grades one answer and records the outcome on its row.
     *
     * Idempotent: an already-graded answer returns success without calling the API, so the inline
     * attempt and the fallback worker can never double-grade or insert duplicate corrections.
     * A permanent failure marks the answer FAILED; a retryable one leaves it PENDING for the
     * worker or the next app start to pick up.
     */
    suspend fun gradeAnswer(answerId: Long): Result<Unit> {
        val answer = userAnswerDao.getById(answerId)
            ?: return Result.failure(ApiFailure("답안을 찾을 수 없습니다", retryable = false))
        if (answer.gradingStatus == GradingStatus.GRADED) return Result.success(Unit)
        val problem = problemDao.getById(answer.problemId)
            ?: return Result.failure(ApiFailure("문제를 찾을 수 없습니다", retryable = false))

        val result = claudeApiClient.gradeAnswer(problem.koreanText, answer.answerText)

        result.onSuccess { graded ->
            userAnswerDao.update(
                answer.copy(
                    gradingStatus = GradingStatus.GRADED,
                    score = graded.score,
                    overallFeedback = graded.overallFeedback,
                    finalCorrectedVersion = graded.finalCorrectedVersion,
                    gradingError = null
                )
            )
            if (graded.corrections.isNotEmpty()) {
                correctionDao.insertAll(
                    graded.corrections.map { c ->
                        c.toEntity(userAnswerId = answerId, problemId = problem.id)
                    }
                )
            }
        }.onFailure { e ->
            if ((e as? ApiFailure)?.retryable != true) {
                markFailed(answerId, e.message)
            }
        }

        // Deliberately outside onSuccess: Result.onSuccess does not catch, so a throw in here used
        // to escape gradeAnswer entirely — after the answer had already been graded.
        if (result.isSuccess) {
            runCatching { updateDailyProgress() }
        }
        return result.map { }
    }

    /** Marks an answer as permanently failed so the UI can stop showing "채점중". */
    suspend fun markFailed(answerId: Long, reason: String?) {
        val answer = userAnswerDao.getById(answerId) ?: return
        if (answer.gradingStatus == GradingStatus.GRADED) return
        userAnswerDao.update(
            answer.copy(
                gradingStatus = GradingStatus.FAILED,
                gradingError = reason?.takeIf { it.isNotBlank() }
                    ?: "알 수 없는 오류로 채점에 실패했습니다"
            )
        )
    }

    /** Puts a failed or stuck answer back in the queue. Entry point for the 재채점 button. */
    suspend fun retryGrading(answerId: Long) {
        val answer = userAnswerDao.getById(answerId) ?: return
        if (answer.gradingStatus == GradingStatus.GRADED) return
        userAnswerDao.update(
            answer.copy(gradingStatus = GradingStatus.PENDING, gradingError = null)
        )
        // REPLACE, not KEEP: a stale work item for this answer would otherwise silently swallow
        // the request and nothing would happen.
        enqueueGrading(answerId, policy = ExistingWorkPolicy.REPLACE)
    }

    /**
     * Re-queues every answer left at PENDING by a previous session — a crash, a process kill during
     * grading, or a submit whose fallback worker never got registered. Without this, such an answer
     * shows "채점중" forever because nothing is scheduled to finish it.
     */
    suspend fun resumePendingGrading() {
        // Answers submitted within the grace window are still owned by an in-flight submitAnswer
        // and already have a worker scheduled; re-queuing them here would race that attempt.
        val cutoff = System.currentTimeMillis() - INLINE_GRADING_GRACE_SECONDS * 1000
        userAnswerDao.getPending()
            .filter { it.submittedAt < cutoff }
            .forEach { answer ->
                enqueueGrading(answer.id, policy = ExistingWorkPolicy.REPLACE)
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

    private companion object {
        /** Long enough to outlast the inline attempt (30s connect + 90s read OkHttp timeouts). */
        const val INLINE_GRADING_GRACE_SECONDS = 150L
    }
}
