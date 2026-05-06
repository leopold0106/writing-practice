package com.example.writingpractice.ui.notebook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.NotebookEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    onEntryClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: NotebookViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("오답 노트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Period selector
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Period.entries) { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            // Error count chart
            item {
                ErrorCountChart(
                    errorCounts = state.errorCounts,
                    period = state.selectedPeriod
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // Search
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setQuery,
                    label = { Text("검색") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
            }

            // Filter chips (by error type)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("전체") }
                        )
                    }
                    items(ErrorType.entries) { type ->
                        FilterChip(
                            selected = state.selectedFilter == type,
                            onClick = {
                                viewModel.setFilter(
                                    if (state.selectedFilter == type) null else type
                                )
                            },
                            label = { Text(type.displayName()) }
                        )
                    }
                }
            }

            // Sort chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SortOrder.entries) { order ->
                        FilterChip(
                            selected = state.sortOrder == order,
                            onClick = { viewModel.setSortOrder(order) },
                            label = { Text(order.label) }
                        )
                    }
                }
            }

            // Empty state
            if (state.filteredEntries.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.entries.isEmpty()) "아직 오답이 없습니다"
                            else "검색 결과가 없습니다",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Entry list
            items(state.filteredEntries, key = { it.problemId }) { entry ->
                NotebookEntryCard(
                    entry = entry,
                    onClick = { onEntryClick(entry.problemId) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorCountChart(errorCounts: Map<ErrorType, Int>, period: Period) {
    val totalCount = errorCounts.values.sum()
    val maxCount = errorCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "오류 유형별 현황",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "총 ${totalCount}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ErrorType.entries.forEach { type ->
                    BarItem(
                        label = type.displayName(),
                        count = errorCounts[type] ?: 0,
                        maxCount = maxCount,
                        color = type.barColor()
                    )
                }
            }
        }
    }
}

@Composable
private fun BarItem(label: String, count: Int, maxCount: Int, color: Color) {
    val animatedFraction by animateFloatAsState(
        targetValue = if (maxCount > 0) count.toFloat() / maxCount else 0f,
        animationSpec = spring(),
        label = "barFraction"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Text(
            if (count > 0) "$count" else "0",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background track
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(1.dp)
                    )
            )
            // Animated bar
            if (animatedFraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp * animatedFraction)
                        .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun NotebookEntryCard(
    entry: NotebookEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.M.d a h:mm", Locale.KOREAN) }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lv.${entry.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    entry.latestScore?.let { score ->
                        Text(
                            "${score}점",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                score >= 80 -> Color(0xFF388E3C)
                                score >= 60 -> Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${entry.corrections.size}개 오류",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (entry.latestAnsweredAt > 0L) {
                        Text(
                            dateFormat.format(Date(entry.latestAnsweredAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                entry.koreanText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            entry.corrections.take(2).forEach { c ->
                Text(
                    "• ${c.originalSentence} → ${c.correctedSentence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.corrections.size > 2) {
                Text(
                    "외 ${entry.corrections.size - 2}개 더 보기",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun ErrorType.displayName() = when (this) {
    ErrorType.GRAMMAR -> "문법"
    ErrorType.VOCABULARY -> "어휘"
    ErrorType.STRUCTURE -> "구조"
    ErrorType.PUNCTUATION -> "구두점"
    ErrorType.SPELLING -> "철자"
}

private fun ErrorType.barColor() = when (this) {
    ErrorType.GRAMMAR -> Color(0xFFEF5350)
    ErrorType.VOCABULARY -> Color(0xFF42A5F5)
    ErrorType.STRUCTURE -> Color(0xFF66BB6A)
    ErrorType.PUNCTUATION -> Color(0xFFFFA726)
    ErrorType.SPELLING -> Color(0xFFAB47BC)
}
