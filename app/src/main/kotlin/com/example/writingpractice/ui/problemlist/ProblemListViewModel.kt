package com.example.writingpractice.ui.problemlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.model.ProblemWithStatus
import com.example.writingpractice.data.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProblemListUiState(
    val problems: List<ProblemWithStatus> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProblemListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val problemRepository: ProblemRepository,
    private val userAnswerDao: UserAnswerDao
) : ViewModel() {

    val level: Int = savedStateHandle["level"] ?: 1

    val uiState: StateFlow<ProblemListUiState> =
        problemRepository.observeByLevel(level).map { problems ->
            val withStatus = problems.map { problem ->
                val count = userAnswerDao.countGradedForProblem(problem.id)
                ProblemWithStatus(
                    problem = problem,
                    latestScore = null,
                    attemptCount = count
                )
            }
            ProblemListUiState(problems = withStatus, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProblemListUiState()
        )
}
