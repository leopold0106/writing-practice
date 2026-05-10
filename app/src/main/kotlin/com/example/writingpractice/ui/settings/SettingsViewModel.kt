package com.example.writingpractice.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.remote.AppUpdateChecker
import com.example.writingpractice.data.remote.ClaudeApiClient
import com.example.writingpractice.data.repository.BackupRepository
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
import java.io.File
import javax.inject.Inject

data class ModelOption(val id: String, val label: String, val description: String)

sealed class ApiKeyStatus {
    object Idle : ApiKeyStatus()
    object Checking : ApiKeyStatus()
    object Valid : ApiKeyStatus()
    data class Invalid(val reason: String) : ApiKeyStatus()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val version: String, val downloadUrl: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val apkPath: String) : UpdateState()
    data class NeedInstallPermission(val apkPath: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

sealed class BackupState {
    object Idle : BackupState()
    object Exporting : BackupState()
    data class ExportDone(val uri: Uri) : BackupState()
    object Importing : BackupState()
    data class ImportDone(val count: Int) : BackupState()
    data class Error(val message: String) : BackupState()
}

data class SettingsUiState(
    val dailyGoal: Int = 5,
    val notificationEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false,
    val selectedModel: String = SettingsRepository.DEFAULT_MODEL
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val claudeApiClient: ClaudeApiClient,
    private val appUpdateChecker: AppUpdateChecker,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _apiKeyVisible = MutableStateFlow(false)
    private val _apiKeyStatus = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Idle)
    val apiKeyStatus: StateFlow<ApiKeyStatus> = _apiKeyStatus.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(settingsRepository.dailyGoal, settingsRepository.notificationEnabled) { g, e -> g to e },
        combine(settingsRepository.notificationHour, settingsRepository.notificationMinute) { h, m -> h to m },
        combine(settingsRepository.apiKey, _apiKeyVisible) { k, v -> k to v },
        settingsRepository.selectedModel
    ) { goalAndEnabled, hourAndMinute, keyAndVisible, model ->
        val (goal, enabled) = goalAndEnabled
        val (hour, minute) = hourAndMinute
        val (key, visible) = keyAndVisible
        SettingsUiState(
            dailyGoal = goal,
            notificationEnabled = enabled,
            notificationHour = hour,
            notificationMinute = minute,
            apiKey = key,
            apiKeyVisible = visible,
            selectedModel = model
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

    fun sendTestNotification() = viewModelScope.launch {
        val goal = settingsRepository.dailyGoal.first()
        NotificationHelper.showDailyReminder(context, 0, goal)
    }

    fun toggleApiKeyVisibility() {
        _apiKeyVisible.value = !_apiKeyVisible.value
    }

    fun checkForUpdate() = viewModelScope.launch {
        _updateState.value = UpdateState.Checking
        appUpdateChecker.checkForUpdate()
            .onSuccess { info ->
                _updateState.value = if (info != null)
                    UpdateState.UpdateAvailable(info.version, info.downloadUrl)
                else
                    UpdateState.UpToDate
            }
            .onFailure { e ->
                _updateState.value = UpdateState.Error(e.message ?: "알 수 없는 오류")
            }
    }

    fun downloadAndInstall(downloadUrl: String) = viewModelScope.launch {
        _updateState.value = UpdateState.Downloading(-1f)
        appUpdateChecker.downloadApk(downloadUrl) { progress ->
            _updateState.value = UpdateState.Downloading(progress)
        }.onSuccess { apkFile ->
            if (appUpdateChecker.canInstallPackages()) {
                _updateState.value = UpdateState.ReadyToInstall(apkFile.absolutePath)
                appUpdateChecker.installApk(apkFile)
            } else {
                _updateState.value = UpdateState.NeedInstallPermission(apkFile.absolutePath)
                appUpdateChecker.openInstallPermissionSettings()
            }
        }.onFailure { e ->
            _updateState.value = UpdateState.Error(e.message ?: "다운로드 실패")
        }
    }

    fun retryInstall(apkPath: String) {
        val apkFile = File(apkPath)
        if (apkFile.exists()) appUpdateChecker.installApk(apkFile)
    }

    fun resetUpdateState() { _updateState.value = UpdateState.Idle }

    fun setSelectedModel(model: String) = viewModelScope.launch {
        settingsRepository.setSelectedModel(model)
    }

    fun exportBackup() = viewModelScope.launch {
        _backupState.value = BackupState.Exporting
        backupRepository.exportBackup()
            .onSuccess { uri -> _backupState.value = BackupState.ExportDone(uri) }
            .onFailure { e -> _backupState.value = BackupState.Error(e.message ?: "내보내기 실패") }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        _backupState.value = BackupState.Importing
        backupRepository.importBackup(uri)
            .onSuccess { count -> _backupState.value = BackupState.ImportDone(count) }
            .onFailure { e -> _backupState.value = BackupState.Error(e.message ?: "가져오기 실패") }
    }

    fun resetBackupState() { _backupState.value = BackupState.Idle }

    companion object {
        val MODELS = listOf(
            ModelOption("claude-haiku-4-5-20251001", "Haiku 4.5", "빠름 · 저비용"),
            ModelOption("claude-sonnet-4-6", "Sonnet 4.6", "균형 · 권장"),
            ModelOption("claude-opus-4-7", "Opus 4.7", "강력 · 고비용")
        )
    }
}
