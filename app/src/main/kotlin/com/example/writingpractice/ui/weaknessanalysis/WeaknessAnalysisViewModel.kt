package com.example.writingpractice.ui.weaknessanalysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.model.WeaknessAnalysis
import com.example.writingpractice.data.repository.ProblemRepository
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.data.repository.WeaknessAnalysisRepository
import com.example.writingpractice.ui.common.Period
import com.example.writingpractice.ui.home.ApiStatus
import com.example.writingpractice.ui.home.GenerateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeaknessAnalysisUiState(
    val history: List<WeaknessAnalysis> = emptyList(),
    val totalCorrections: Int = 0,
    val apiStatus: ApiStatus = ApiStatus.UNKNOWN,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class WeaknessAnalysisViewModel @Inject constructor(
    private val weaknessRepository: WeaknessAnalysisRepository,
    private val problemRepository: ProblemRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _totalCorrections = MutableStateFlow(0)

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    private val apiStatusFlow =
        combine(settingsRepository.apiKey, settingsRepository.apiKeyValidated) { key, validated ->
            when {
                key.isEmpty() -> ApiStatus.UNKNOWN
                validated == true -> ApiStatus.VALID
                validated == false -> ApiStatus.INVALID
                else -> ApiStatus.UNKNOWN
            }
        }

    val uiState: StateFlow<WeaknessAnalysisUiState> = combine(
        weaknessRepository.observeHistory(),
        _totalCorrections,
        apiStatusFlow,
        combine(weaknessRepository.isAnalyzing, weaknessRepository.lastError) { a, e -> a to e }
    ) { history, total, apiStatus, (analyzing, err) ->
        WeaknessAnalysisUiState(
            history = history,
            totalCorrections = total,
            apiStatus = apiStatus,
            isAnalyzing = analyzing,
            error = err,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeaknessAnalysisUiState()
    )

    init {
        // Refresh total corrections whenever an analysis completes (initial false also triggers).
        viewModelScope.launch {
            weaknessRepository.isAnalyzing.collect { analyzing ->
                if (!analyzing) {
                    _totalCorrections.value =
                        weaknessRepository.countCorrectionsForPeriod(Period.ALL)
                }
            }
        }
    }

    fun analyzeNow() {
        weaknessRepository.triggerAnalyze(Period.ALL)
    }

    fun generateProblemsFromWeaknesses(analysis: WeaknessAnalysis) {
        if (_generateState.value is GenerateState.Loading) return
        viewModelScope.launch {
            val level = analysis.recommendedLevel.coerceIn(1, 7)
            _generateState.value = GenerateState.Loading(level)
            val weaknessTypes = analysis.weaknessPoints
                .map { it.errorType.name }
                .distinct()
                .take(3)
            val result = problemRepository.generateAndInsert(level, weaknessTypes)
            _generateState.value = if (result.isSuccess) {
                GenerateState.Success(level, result.getOrDefault(emptyList()).size)
            } else {
                GenerateState.Error(level, result.exceptionOrNull()?.message ?: "알 수 없는 오류")
            }
        }
    }

    fun resetGenerateState() {
        _generateState.value = GenerateState.Idle
    }

    fun dismissError() {
        weaknessRepository.dismissError()
    }
}
