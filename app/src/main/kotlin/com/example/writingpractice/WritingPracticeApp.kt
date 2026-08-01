package com.example.writingpractice

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.writingpractice.data.repository.PracticeRepository
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
import javax.inject.Provider

@HiltAndroidApp
class WritingPracticeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var problemRepository: ProblemRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    // A Provider, not the repository itself: constructing PracticeRepository calls
    // WorkManager.getInstance(), whose on-demand init reads workManagerConfiguration above. Eager
    // field injection would make startup depend on the order Hilt assigns these fields.
    @Inject lateinit var practiceRepository: Provider<PracticeRepository>

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
            // Pick up answers a previous session left mid-grading, so they can't show "채점중" forever.
            practiceRepository.get().resumePendingGrading()
            val enabled = settingsRepository.notificationEnabled.first()
            if (enabled) {
                val hour = settingsRepository.notificationHour.first()
                val minute = settingsRepository.notificationMinute.first()
                NotificationHelper.scheduleDaily(this@WritingPracticeApp, hour, minute)
            }
        }
    }
}
