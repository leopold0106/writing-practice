package com.example.writingpractice.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.Correction
import com.example.writingpractice.ui.theme.ScoreGreen
import com.example.writingpractice.ui.theme.ScoreRed
import com.example.writingpractice.ui.theme.ScoreYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    answerId: Long,
    onNextProblem: (Int) -> Unit,
    onHome: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val autoAnalysisState by viewModel.autoAnalysisState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("채점 결과") }) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pending banner — replaces score card while waiting for grading
            if (state.isPending) {
                PendingBanner()
            } else {
                state.score?.let { score ->
                    ScoreCard(score = score, feedback = state.overallFeedback)
                }
            }

            state.problem?.let { problem ->
                InfoCard(title = "한국어 문제") {
                    Text(problem.koreanText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            InfoCard(title = "내 답변") {
                Text(state.userAnswer, style = MaterialTheme.typography.bodyMedium)
            }

            if (!state.isPending) {
                if (state.corrections.isNotEmpty()) {
                    InfoCard(title = "수정 사항 (${state.corrections.size}개)") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.corrections.forEach { correction ->
                                CorrectionItem(correction)
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ScoreGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            "완벽한 답변입니다!",
                            modifier = Modifier.padding(16.dp),
                            color = ScoreGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (state.finalCorrectedVersion.isNotBlank()) {
                    InfoCard(title = "최종 수정본") {
                        Text(
                            state.finalCorrectedVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (autoAnalysisState != AutoAnalysisState.Idle) {
                AutoAnalysisBanner(autoAnalysisState)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f)) {
                    Text("홈으로")
                }
                if (!state.isPending) {
                    Button(
                        onClick = { onNextProblem(state.problem?.level ?: 1) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("다음 문제")
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoAnalysisBanner(state: AutoAnalysisState) {
    val isRunning = state is AutoAnalysisState.Running
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1565C0)
                )
            }
            Text(
                text = if (isRunning) "30문제 달성! 약점 분석 자동 업데이트 중..."
                       else "약점 분석이 업데이트되었습니다",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1565C0)
            )
        }
    }
}

@Composable
private fun PendingBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ScoreYellow.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = ScoreYellow
            )
            Column {
                Text(
                    "채점 대기 중",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
                Text(
                    "인터넷 연결 시 자동으로 채점됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Int, feedback: String) {
    val color = when {
        score >= 80 -> ScoreGreen
        score >= 50 -> ScoreYellow
        else -> ScoreRed
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text("점", style = MaterialTheme.typography.titleSmall, color = color)
            if (feedback.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(feedback, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun CorrectionItem(correction: Correction) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            correction.originalSentence,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = TextDecoration.LineThrough,
            color = ScoreRed
        )
        Text(
            correction.correctedSentence,
            style = MaterialTheme.typography.bodySmall,
            color = ScoreGreen,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            correction.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ErrorTypeChip(correction.errorType)
    }
}

@Composable
private fun ErrorTypeChip(errorType: ErrorType) {
    val (label, color) = when (errorType) {
        ErrorType.GRAMMAR -> "문법" to Color(0xFF9C27B0)
        ErrorType.VOCABULARY -> "어휘" to Color(0xFF2196F3)
        ErrorType.STRUCTURE -> "구조" to Color(0xFFFF9800)
        ErrorType.PUNCTUATION -> "구두점" to Color(0xFF607D8B)
        ErrorType.SPELLING -> "철자" to ScoreRed
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    )
}
