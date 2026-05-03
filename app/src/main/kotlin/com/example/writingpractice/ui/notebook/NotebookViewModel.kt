package com.example.writingpractice.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.NotebookEntry
import com.example.writingpractice.data.repository.CorrectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NotebookUiState(
    val entries: List<NotebookEntry> = emptyList(),
    val filteredEntries: List<NotebookEntry> = emptyList(),
    val selectedFilter: ErrorType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class NotebookViewModel @Inject constructor(
    correctionRepository: CorrectionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<ErrorType?>(null)
    private val _query = MutableStateFlow("")

    val uiState: StateFlow<NotebookUiState> = combine(
        correctionRepository.observeNotebookEntries(),
        _filter,
        _query
    ) { entries, filter, query ->
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

        NotebookUiState(
            entries = entries,
            filteredEntries = filtered,
            selectedFilter = filter,
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
}
