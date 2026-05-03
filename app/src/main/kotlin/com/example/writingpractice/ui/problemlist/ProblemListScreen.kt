package com.example.writingpractice.ui.problemlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.data.model.ProblemWithStatus
import com.example.writingpractice.ui.theme.ScoreGreen
import com.example.writingpractice.ui.theme.ScoreYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemListScreen(
    level: Int,
    onProblemClick: (Long) -> Unit,
    onStartRandom: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProblemListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("레벨 $level 문제 목록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Button(
                onClick = onStartRandom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(" 랜덤 문제 풀기", modifier = Modifier.padding(start = 4.dp))
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(state.problems, key = { it.problem.id }) { item ->
                        ProblemRow(item = item, onClick = { onProblemClick(item.problem.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemRow(item: ProblemWithStatus, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.problem.koreanText.take(50).let {
                if (item.problem.koreanText.length > 50) "$it…" else it
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        StatusChip(item)
    }
}

@Composable
private fun StatusChip(item: ProblemWithStatus) {
    when {
        item.isGraded -> SuggestionChip(
            onClick = {},
            label = { Text("완료") },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = ScoreGreen.copy(alpha = 0.15f)
            )
        )
        item.isAttempted -> SuggestionChip(
            onClick = {},
            label = { Text("채점중") },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = ScoreYellow.copy(alpha = 0.15f)
            )
        )
        else -> SuggestionChip(
            onClick = {},
            label = { Text("새 문제") }
        )
    }
}
