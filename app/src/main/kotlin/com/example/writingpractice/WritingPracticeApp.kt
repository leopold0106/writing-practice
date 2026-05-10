package com.example.writingpractice

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.writingpractice.data.repository.ProblemRepository
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WritingPracticeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var problemRepository: ProblemRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        appScope.launch {
            problemRepository.seedIfNeeded()
            val enabled = settingsRepository.notificationEnabled.first()
            if (enabled) {
                val hour = settingsRepository.notificationHour.first()
                val minute = settingsRepository.notificationMinute.first()
                NotificationHelper.scheduleDaily(this@WritingPracticeApp, hour, minute)
            }
        }
    }
}
