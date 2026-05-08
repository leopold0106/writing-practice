package com.example.writingpractice.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.repository.PracticeRepository
import com.example.writingpractice.data.repository.ProblemRepository
import com.example.writingpractice.data.repository.SettingsRepository
import com.example.writingpractice.data.repository.WeaknessAnalysisRepository
import com.example.writingpractice.ui.common.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AutoAnalysisState {
    object Idle : AutoAnalysisState()
    object Running : AutoAnalysisState()
    object Done : AutoAnalysisState()
}

data class ResultUiState(
    val problem: Problem? = null,
    val userAnswer: String = "",
    val score: Int? = null,
    val overallFeedback: String = "",
    val corrections: List<Correction> = emptyList(),
    val finalCorrectedVersion: String = "",
    val isLoading: Boolean = true,
    val isPending: Boolean = false
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val problemRepository: ProblemRepository,
    private val practiceRepository: PracticeRepository,
    private val userAnswerDao: UserAnswerDao,
    private val correctionDao: CorrectionDao,
    private val weaknessAnalysisRepository: WeaknessAnalysisRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val answerId: Long = savedStateHandle["answerId"] ?: -1L

    private val _problem = MutableStateFlow<Problem?>(null)
    private val _autoAnalysisState = MutableStateFlow<AutoAnalysisState>(AutoAnalysisState.Idle)
    val autoAnalysisState: StateFlow<AutoAnalysisState> = _autoAnalysisState.asStateFlow()

    private var wasPending = false
    private var hasCheckedAutoAnalysis = false

    val uiState: StateFlow<ResultUiState> = combine(
        practiceRepository.observeAnswer(answerId),
        practiceRepository.observeCorrectionsForAnswer(answerId),
        _problem
    ) { answer, corrections, problem ->
        if (answer == null) return@combine ResultUiState(isLoading = true)
        if (problem == null) {
            val p = problemRepository.getById(answer.problemId)
            _problem.value = p
            return@combine ResultUiState(isLoading = true)
        }
        ResultUiState(
            problem = problem,
            userAnswer = answer.answerText,
            score = answer.score,
            overallFeedback = answer.overallFeedback ?: "",
            corrections = corrections,
            finalCorrectedVersion = answer.finalCorrectedVersion ?: "",
            isLoading = false,
            isPending = answer.score == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResultUiState()
    )

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (!state.isLoading && !hasCheckedAutoAnalysis) {
                    if (state.isPending) {
                        wasPending = true
                    } else if (state.score != null) {
                        // Trigger whether screen opened mid-grading or with already-graded answer
                        hasCheckedAutoAnalysis = true
                        checkAndAutoAnalyze()
                    }
                }
            }
        }
    }

    private suspend fun checkAndAutoAnalyze() {
        val total = correctionDao.countCorrectionsAfter(0L)
        val lastCount = settingsRepository.getLastAutoAnalyzedCount()
        if (total / 30 > lastCount / 30) {
            _autoAnalysisState.value = AutoAnalysisState.Running
            val result = weaknessAnalysisRepository.analyze(Period.ALL)
            if (result.isSuccess) {
                settingsRepository.setLastAutoAnalyzedCount(total)
            }
            _autoAnalysisState.value = AutoAnalysisState.Done
        }
    }
}
