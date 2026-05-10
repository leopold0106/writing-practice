package com.example.writingpractice.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        return practiceRepository.gradeAnswer(answerId).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val KEY_ANSWER_ID = "answer_id"
    }
}
