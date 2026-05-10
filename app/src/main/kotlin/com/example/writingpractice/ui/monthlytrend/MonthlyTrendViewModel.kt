package com.example.writingpractice.ui.monthlytrend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.model.MonthlySnapshot
import com.example.writingpractice.data.repository.MonthlyAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MonthlyTrendUiState(
    val snapshots: List<MonthlySnapshot> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MonthlyTrendViewModel @Inject constructor(
    monthlyAnalysisRepository: MonthlyAnalysisRepository
) : ViewModel() {

    val uiState: StateFlow<MonthlyTrendUiState> =
        monthlyAnalysisRepository.observeAll().map { snapshots ->
            MonthlyTrendUiState(snapshots = snapshots, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyTrendUiState()
        )
}
