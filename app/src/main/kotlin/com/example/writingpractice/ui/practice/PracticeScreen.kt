package com.example.writingpractice.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    level: Int,
    problemId: Long?,
    onResultReady: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(level, problemId) {
        viewModel.loadProblem(level, problemId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect { answerId ->
            onResultReady(answerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("레벨 $level 연습") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is PracticeUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is PracticeUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(s.message, color = MaterialTheme.colorScheme.error) }

            is PracticeUiState.Writing -> WritingContent(
                koreanText = s.problem.koreanText,
                answer = s.answer,
                onAnswerChange = viewModel::onAnswerChange,
                onSubmit = viewModel::submitAnswer,
                isSubmitting = false,
                modifier = Modifier.padding(padding)
            )

            is PracticeUiState.Submitting -> GradingContent(
                koreanText = s.problem.koreanText,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun WritingContent(
    koreanText: String,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "한국어 문제",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    koreanText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
        }

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text("영어로 번역하세요") },
            modifier = Modifier.fillMaxWidth().height(180.dp),
            maxLines = 10
        )

        Button(
            onClick = onSubmit,
            enabled = answer.isNotBlank() && !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("제출하기")
        }
    }
}

@Composable
private fun GradingContent(koreanText: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("한국어 문제", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(koreanText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "답변을 검토하고 있습니다…",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "네트워크 연결 시 자동으로 채점됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
