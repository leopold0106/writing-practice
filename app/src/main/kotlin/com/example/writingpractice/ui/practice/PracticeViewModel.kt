package com.example.writingpractice.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.repository.PracticeRepository
import com.example.writingpractice.data.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PracticeUiState {
    data object Loading : PracticeUiState()
    data class Writing(val problem: Problem, val answer: String = "") : PracticeUiState()
    data class Submitting(val problem: Problem) : PracticeUiState()
    data class Grading(val problem: Problem, val answerId: Long) : PracticeUiState()
    data class Pending(val problem: Problem, val answerId: Long) : PracticeUiState()
    data class Error(val message: String) : PracticeUiState()
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val problemRepository: ProblemRepository,
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val _navigateToResult = Channel<Long>(Channel.BUFFERED)
    val navigateToResult = _navigateToResult.receiveAsFlow()

    fun loadProblem(level: Int, problemId: Long?) {
        if (_uiState.value !is PracticeUiState.Loading) return
        viewModelScope.launch {
            val problem = if (problemId != null) {
                problemRepository.getById(problemId)
            } else {
                problemRepository.getNextProblem(level)
            }
            if (problem == null) {
                _uiState.value = PracticeUiState.Error("문제를 불러올 수 없습니다.")
            } else {
                val draft = practiceRepository.getDraft(problem.id)
                _uiState.value = PracticeUiState.Writing(problem, draft)
            }
        }
    }

    fun onAnswerChange(text: String) {
        val current = _uiState.value as? PracticeUiState.Writing ?: return
        practiceRepository.saveDraft(current.problem.id, text)
        _uiState.update { PracticeUiState.Writing(current.problem, text) }
    }

    fun submitAnswer() {
        val current = _uiState.value as? PracticeUiState.Writing ?: return
        if (current.answer.isBlank()) return
        viewModelScope.launch {
            _uiState.value = PracticeUiState.Submitting(current.problem)
            val answerId = practiceRepository.submitAnswer(current.problem.id, current.answer)
            practiceRepository.clearDraft(current.problem.id)
            val status = practiceRepository.getGradingStatus(answerId)
            if (status == GradingStatus.GRADED) {
                _navigateToResult.send(answerId)
            } else {
                _uiState.value = PracticeUiState.Pending(current.problem, answerId)
                observeGrading(answerId)
            }
        }
    }

    private fun observeGrading(answerId: Long) {
        practiceRepository.observeGradingStatus(answerId)
            .filterNotNull()
            .onEach { status ->
                if (status == GradingStatus.GRADED) {
                    _navigateToResult.send(answerId)
                }
            }
            .launchIn(viewModelScope)
    }
}
