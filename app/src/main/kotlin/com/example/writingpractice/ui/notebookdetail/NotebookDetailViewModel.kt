package com.example.writingpractice.ui.notebookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.repository.CorrectionRepository
import com.example.writingpractice.data.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotebookDetailUiState(
    val problem: Problem? = null,
    val corrections: List<Correction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotebookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val problemRepository: ProblemRepository,
    private val correctionRepository: CorrectionRepository
) : ViewModel() {

    val problemId: Long = savedStateHandle["problemId"] ?: -1L

    private val _problem = MutableStateFlow<Problem?>(null)

    init {
        viewModelScope.launch {
            _problem.value = problemRepository.getById(problemId)
        }
        viewModelScope.launch {
            correctionRepository.markProblemReviewed(problemId)
        }
    }

    val uiState: StateFlow<NotebookDetailUiState> = combine(
        _problem,
        correctionRepository.observeCorrectionsForProblem(problemId)
    ) { problem, corrections ->
        NotebookDetailUiState(
            problem = problem,
            corrections = corrections,
            isLoading = problem == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotebookDetailUiState()
    )
}
