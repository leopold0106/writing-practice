package com.example.writingpractice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.repository.CorrectionRepository
import com.example.writingpractice.data.repository.PracticeRepository
import com.example.writingpractice.data.repository.ProblemRepository
import com.example.writingpractice.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ApiStatus { UNKNOWN, VALID, INVALID }

sealed class GenerateState {
    object Idle : GenerateState()
    data class Loading(val level: Int) : GenerateState()
    data class Success(val level: Int) : GenerateState()
    data class Error(val level: Int, val message: String) : GenerateState()
}

data class HomeUiState(
    val todaySolved: Int = 0,
    val dailyGoal: Int = 5,
    val unreviewedCorrections: Int = 0,
    val apiStatus: ApiStatus = ApiStatus.UNKNOWN,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val correctionRepository: CorrectionRepository,
    private val settingsRepository: SettingsRepository,
    private val problemRepository: ProblemRepository,
) : ViewModel() {

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    fun generateProblem(level: Int) {
        viewModelScope.launch {
            _generateState.value = GenerateState.Loading(level)
            val result = problemRepository.generateAndInsert(level)
            _generateState.value = if (result.isSuccess) {
                GenerateState.Success(level)
            } else {
                GenerateState.Error(level, result.exceptionOrNull()?.message ?: "알 수 없는 오류")
            }
        }
    }

    fun resetGenerateState() {
        _generateState.value = GenerateState.Idle
    }

    val uiState: StateFlow<HomeUiState> = combine(
        practiceRepository.observeTodaySolvedCount(),
        settingsRepository.dailyGoal,
        correctionRepository.observeUnreviewedCount(),
        combine(settingsRepository.apiKey, settingsRepository.apiKeyValidated) { key, validated ->
            when {
                key.isEmpty() -> ApiStatus.UNKNOWN
                validated == true -> ApiStatus.VALID
                validated == false -> ApiStatus.INVALID
                else -> ApiStatus.UNKNOWN
            }
        }
    ) { solved, goal, unreviewed, apiStatus ->
        HomeUiState(
            todaySolved = solved,
            dailyGoal = goal,
            unreviewedCorrections = unreviewed,
            apiStatus = apiStatus,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
