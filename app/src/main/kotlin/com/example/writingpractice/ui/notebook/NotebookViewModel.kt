package com.example.writingpractice.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.NotebookEntry
import com.example.writingpractice.data.repository.CorrectionRepository
import com.example.writingpractice.ui.common.Period
import com.example.writingpractice.ui.common.sinceMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class SortOrder(val label: String) {
    BY_TIME("시간순"),
    BY_SCORE("점수순"),
    BY_LEVEL("레벨순"),
    BY_ERROR_TYPE("오류 유형순")
}

data class NotebookUiState(
    val entries: List<NotebookEntry> = emptyList(),
    val filteredEntries: List<NotebookEntry> = emptyList(),
    val selectedFilter: ErrorType? = null,
    val sortOrder: SortOrder = SortOrder.BY_TIME,
    val selectedPeriod: Period = Period.MONTH,
    val errorCounts: Map<ErrorType, Int> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val correctionRepository: CorrectionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<ErrorType?>(null)
    private val _query = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.BY_TIME)
    private val _period = MutableStateFlow(Period.MONTH)

    private val _periodAndCounts: Flow<Pair<Period, Map<ErrorType, Int>>> =
        _period.flatMapLatest { period ->
            correctionRepository.observeErrorCounts(period.sinceMs).map { period to it }
        }

    val uiState: StateFlow<NotebookUiState> = combine(
        correctionRepository.observeNotebookEntries(),
        _filter,
        _query,
        _sortOrder,
        _periodAndCounts
    ) { entries, filter, query, sortOrder, periodData ->
        val (period, errorCounts) = periodData

        val filtered = entries
            .map { entry ->
                entry.copy(
                    corrections = entry.corrections.filter { c ->
                        (filter == null || c.errorType == filter) &&
                                (query.isBlank() ||
                                        c.originalSentence.contains(query, ignoreCase = true) ||
                                        c.correctedSentence.contains(query, ignoreCase = true) ||
                                        entry.koreanText.contains(query, ignoreCase = true))
                    }
                )
            }
            .filter { it.corrections.isNotEmpty() }
            .sortedWith(
                when (sortOrder) {
                    SortOrder.BY_TIME -> compareByDescending { it.latestAnsweredAt }
                    SortOrder.BY_SCORE -> compareByDescending { it.latestScore ?: -1 }
                    SortOrder.BY_LEVEL -> compareBy { it.level }
                    SortOrder.BY_ERROR_TYPE -> compareBy { entry ->
                        entry.corrections
                            .groupBy { it.errorType }
                            .maxByOrNull { (_, list) -> list.size }
                            ?.key?.ordinal ?: Int.MAX_VALUE
                    }
                }
            )

        NotebookUiState(
            entries = entries,
            filteredEntries = filtered,
            selectedFilter = filter,
            sortOrder = sortOrder,
            selectedPeriod = period,
            errorCounts = errorCounts,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotebookUiState()
    )

    fun setFilter(filter: ErrorType?) { _filter.update { filter } }
    fun setQuery(query: String) { _query.update { query } }
    fun setSortOrder(order: SortOrder) { _sortOrder.update { order } }
    fun setPeriod(period: Period) { _period.update { period } }
}
