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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class WeaknessAnalysisUiState(
    val period: Period = Period.MONTH,
    val latestAnalysis: WeaknessAnalysis? = null,
    val correctionCount: Int = 0,
    val apiStatus: ApiStatus = ApiStatus.UNKNOWN,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class WeaknessAnalysisViewModel @Inject constructor(
    private val weaknessRepository: WeaknessAnalysisRepository,
    private val problemRepository: ProblemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _period = MutableStateFlow(Period.MONTH)
    private val _isAnalyzing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _correctionCount = MutableStateFlow(0)
    private val analysisMutex = Mutex()

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

    private val latestFlow = _period.flatMapLatest { period ->
        weaknessRepository.observeLatestForPeriod(period)
    }

    val uiState: StateFlow<WeaknessAnalysisUiState> = combine(
        _period,
        latestFlow,
        _correctionCount,
        apiStatusFlow,
        combine(_isAnalyzing, _error) { analyzing, err -> analyzing to err }
    ) { period, latest, count, apiStatus, (analyzing, err) ->
        WeaknessAnalysisUiState(
            period = period,
            latestAnalysis = latest,
            correctionCount = count,
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
        // Refresh correction count whenever the period changes
        viewModelScope.launch {
            _period.collect { period ->
                _correctionCount.value = weaknessRepository.countCorrectionsForPeriod(period)
            }
        }
    }

    fun setPeriod(period: Period) {
        _period.update { period }
        _error.update { null }
    }

    fun analyze() {
        if (_isAnalyzing.value) return
        viewModelScope.launch {
            analysisMutex.withLock {
                _isAnalyzing.value = true
                _error.value = null
                val period = _period.value
                weaknessRepository.analyze(period)
                    .onFailure { e -> _error.value = e.message ?: "알 수 없는 오류" }
                _correctionCount.value = weaknessRepository.countCorrectionsForPeriod(period)
                _isAnalyzing.value = false
            }
        }
    }

    fun generateProblemsFromWeaknesses() {
        val analysis = uiState.value.latestAnalysis ?: return
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
        _error.value = null
    }
}
