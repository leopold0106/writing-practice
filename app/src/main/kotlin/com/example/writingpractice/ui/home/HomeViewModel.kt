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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ApiStatus { UNKNOWN, VALID, INVALID }

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
) : ViewModel() {

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
