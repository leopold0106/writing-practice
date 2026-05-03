package com.example.writingpractice.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.writingpractice.data.local.db.dao.ProgressDao
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.util.DateTimeUtil
import com.example.writingpractice.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val progressDao: ProgressDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.notificationEnabled.first()
        if (!enabled) return Result.success()

        val goal = settingsRepository.dailyGoal.first()
        val todayProgress = progressDao.getForDate(DateTimeUtil.todayIso())
        val solved = todayProgress?.problemsSolved ?: 0

        if (solved < goal) {
            NotificationHelper.showDailyReminder(applicationContext, solved, goal)
        }
        return Result.success()
    }
}
