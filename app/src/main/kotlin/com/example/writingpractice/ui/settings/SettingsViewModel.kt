package com.example.writingpractice.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.remote.ClaudeApiClient
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ApiKeyStatus {
    object Idle : ApiKeyStatus()
    object Checking : ApiKeyStatus()
    object Valid : ApiKeyStatus()
    data class Invalid(val reason: String) : ApiKeyStatus()
}

data class SettingsUiState(
    val dailyGoal: Int = 5,
    val notificationEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val claudeApiClient: ClaudeApiClient
) : ViewModel() {

    private val _apiKeyVisible = MutableStateFlow(false)
    private val _apiKeyStatus = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Idle)
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(settingsRepository.dailyGoal, settingsRepository.notificationEnabled) { g, e -> g to e },
        combine(settingsRepository.notificationHour, settingsRepository.notificationMinute) { h, m -> h to m },
        combine(settingsRepository.apiKey, _apiKeyVisible) { k, v -> k to v }
    ) { (goal, enabled), (hour, minute), (key, visible) ->
        SettingsUiState(
            dailyGoal = goal,
            notificationEnabled = enabled,
            notificationHour = hour,
            notificationMinute = minute,
            apiKey = key,
            apiKeyVisible = visible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    suspend fun loadApiKey(): String = settingsRepository.getApiKey()

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

    fun saveAndValidateApiKey(key: String) = viewModelScope.launch {
        settingsRepository.setApiKey(key)
        _apiKeyStatus.value = ApiKeyStatus.Checking
        val result = claudeApiClient.ping()
        if (result.isSuccess) {
            settingsRepository.setApiKeyValidated(true)
            _apiKeyStatus.value = ApiKeyStatus.Valid
        } else {
            settingsRepository.setApiKeyValidated(false)
            val msg = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
            _apiKeyStatus.value = ApiKeyStatus.Invalid(msg)
        }
    }

    fun toggleApiKeyVisibility() {
        _apiKeyVisible.value = !_apiKeyVisible.value
    }
}
