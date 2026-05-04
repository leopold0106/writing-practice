package com.example.writingpractice.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val dailyGoal: Int = 5,
    val notificationEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false,
    val savedMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKeyVisible = MutableStateFlow(false)
    private val _savedMessage = MutableStateFlow("")

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.dailyGoal,
        settingsRepository.notificationEnabled,
        settingsRepository.notificationHour,
        settingsRepository.notificationMinute,
        settingsRepository.apiKey
    ) { goal, enabled, hour, minute, key ->
        SettingsUiState(
            dailyGoal = goal,
            notificationEnabled = enabled,
            notificationHour = hour,
            notificationMinute = minute,
            apiKey = key,
            apiKeyVisible = _apiKeyVisible.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setDailyGoal(goal: Int) = viewModelScope.launch {
        settingsRepository.setDailyGoal(goal.coerceIn(1, 30))
    }

    fun setNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationEnabled(enabled)
        if (enabled) {
            val h = settingsRepository.notificationHour.first()
            val m = settingsRepository.notificationMinute.first()
            NotificationHelper.scheduleDaily(context, h, m)
        } else {
            NotificationHelper.cancel(context)
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) = viewModelScope.launch {
        settingsRepository.setNotificationTime(hour, minute)
        val enabled = settingsRepository.notificationEnabled.first()
        if (enabled) {
            NotificationHelper.scheduleDaily(context, hour, minute)
        }
    }

    fun setApiKey(key: String) = viewModelScope.launch {
        settingsRepository.setApiKey(key)
    }

    fun toggleApiKeyVisibility() { _apiKeyVisible.value = !_apiKeyVisible.value }

    fun save() = viewModelScope.launch {
        _savedMessage.value = "저장되었습니다"
    }
}
