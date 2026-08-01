package com.example.writingpractice.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.writingpractice.data.remote.ApiFailure
import com.example.writingpractice.data.repository.PracticeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GradeAnswerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val practiceRepository: PracticeRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val answerId = inputData.getLong(KEY_ANSWER_ID, -1L)
        if (answerId == -1L) return Result.failure()

        val error = practiceRepository.gradeAnswer(answerId).exceptionOrNull()
            ?: return Result.success()

        val retryable = (error as? ApiFailure)?.retryable == true
        return if (retryable && runAttemptCount < MAX_ATTEMPTS) {
            Result.retry()
        } else {
            // Give up rather than retry forever: a permanently failing answer (bad API key,
            // unparseable response) must not sit at "채점중" indefinitely.
            practiceRepository.markFailed(answerId, error.message)
            Result.failure()
        }
    }

    companion object {
        const val KEY_ANSWER_ID = "answer_id"
        private const val MAX_ATTEMPTS = 5
    }
}
