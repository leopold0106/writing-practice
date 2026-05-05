package com.example.writingpractice.ui.answerhistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnswerHistoryUiState(
    val problem: Problem? = null,
    val answers: List<UserAnswerEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnswerHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val problemRepository: ProblemRepository,
    private val userAnswerDao: UserAnswerDao
) : ViewModel() {

    val problemId: Long = savedStateHandle["problemId"] ?: -1L

    private val _problem = MutableStateFlow<Problem?>(null)

    val uiState: StateFlow<AnswerHistoryUiState> = combine(
        userAnswerDao.observeForProblem(problemId),
        _problem
    ) { answers, problem ->
        if (problem == null) {
            val p = problemRepository.getById(problemId)
            _problem.value = p
            return@combine AnswerHistoryUiState(isLoading = true)
        }
        AnswerHistoryUiState(problem = problem, answers = answers, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnswerHistoryUiState()
    )
}
