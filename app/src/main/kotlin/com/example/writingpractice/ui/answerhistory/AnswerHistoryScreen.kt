package com.example.writingpractice.ui.answerhistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import com.example.writingpractice.ui.theme.ScoreGreen
import com.example.writingpractice.ui.theme.ScoreRed
import com.example.writingpractice.ui.theme.ScoreYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerHistoryScreen(
    onBack: () -> Unit,
    onAnswerClick: (Long) -> Unit,
    onRePractice: (Int) -> Unit,
    viewModel: AnswerHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("답변 기록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.problem?.let { problem ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "문제",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(problem.koreanText, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onRePractice(problem.level) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                            Text(" 다시 풀기", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                HorizontalDivider()
            }

            if (state.answers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 답변이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(state.answers, key = { it.id }) { answer ->
                        AnswerItem(
                            answer = answer,
                            onClick = { onAnswerClick(answer.id) }
                        )
                    }
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("yyyy.M.d a h:mm", Locale.KOREAN)

@Composable
private fun AnswerItem(answer: UserAnswerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${answer.attemptNumber}번 시도",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(answer.submittedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = answer.answerText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            ScoreChip(answer)
        }
    }
}

@Composable
private fun ScoreChip(answer: UserAnswerEntity) {
    when (answer.gradingStatus) {
        GradingStatus.GRADED -> {
            val score = answer.score ?: 0
            val color = when {
                score >= 80 -> ScoreGreen
                score >= 50 -> ScoreYellow
                else -> ScoreRed
            }
            SuggestionChip(
                onClick = {},
                label = { Text("${score}점", fontWeight = FontWeight.SemiBold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = color.copy(alpha = 0.15f),
                    labelColor = color
                )
            )
        }
        GradingStatus.PENDING -> SuggestionChip(
            onClick = {},
            label = { Text("채점중") },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = ScoreYellow.copy(alpha = 0.15f),
                labelColor = Color(0xFFE65100)
            )
        )
        GradingStatus.OFFLINE_SKIPPED -> SuggestionChip(
            onClick = {},
            label = { Text("오프라인") },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
