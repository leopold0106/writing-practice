package com.example.writingpractice.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.repository.PracticeRepository
import com.example.writingpractice.data.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
    private val userAnswerDao: UserAnswerDao
) : ViewModel() {

    val answerId: Long = savedStateHandle["answerId"] ?: -1L

    private val _problem = MutableStateFlow<Problem?>(null)

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
}
