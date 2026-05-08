package com.example.writingpractice.ui.weaknessanalysis

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.model.OverallLevel
import com.example.writingpractice.data.model.RecommendedPattern
import com.example.writingpractice.data.model.Severity
import com.example.writingpractice.data.model.WeaknessAnalysis
import com.example.writingpractice.data.model.WeaknessPoint
import com.example.writingpractice.ui.home.ApiStatus
import com.example.writingpractice.ui.home.GenerateState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaknessAnalysisScreen(
    onBack: () -> Unit,
    viewModel: WeaknessAnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val generateState by viewModel.generateState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 약점 분석") },
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.apiStatus != ApiStatus.VALID) {
                item { ApiStatusBanner(state.apiStatus) }
            }

            item {
                StatsHeader(
                    totalCorrections = state.totalCorrections,
                    historyCount = state.history.size
                )
            }

            item {
                AnalyzeButton(
                    apiValid = state.apiStatus == ApiStatus.VALID,
                    isAnalyzing = state.isAnalyzing,
                    hasHistory = state.history.isNotEmpty(),
                    correctionCount = state.totalCorrections,
                    onAnalyze = viewModel::analyzeNow
                )
            }

            state.error?.let { errorMsg ->
                item {
                    ErrorSurface(message = errorMsg, onDismiss = viewModel::dismissError)
                }
            }

            if (state.isAnalyzing) {
                item { AnalyzingPlaceholder() }
            }

            if (state.history.isEmpty() && !state.isAnalyzing) {
                item {
                    EmptyHint(
                        text = if (state.totalCorrections == 0)
                            "오답이 쌓이면 30개마다 자동으로 분석됩니다.\n먼저 문제를 풀어보세요."
                        else
                            "아직 분석 기록이 없습니다.\n‘지금 분석하기’를 눌러 첫 분석을 시작하세요."
                    )
                }
            }

            if (state.history.isNotEmpty()) {
                item {
                    Text(
                        "분석 기록",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(
                    items = state.history,
                    key = { it.id }
                ) { analysis ->
                    val idx = state.history.indexOf(analysis)
                    val isLatest = idx == 0
                    val addedSince = if (isLatest) {
                        maxOf(0, state.totalCorrections - analysis.totalCorrections)
                    } else {
                        maxOf(0, state.history[idx - 1].totalCorrections - analysis.totalCorrections)
                    }
                    AnalysisHistoryCard(
                        analysis = analysis,
                        addedSince = addedSince,
                        defaultExpanded = isLatest,
                        showGenerateButton = isLatest,
                        generateState = generateState,
                        onGenerate = { viewModel.generateProblemsFromWeaknesses(analysis) },
                        onResetGenerate = viewModel::resetGenerateState
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiStatusBanner(status: ApiStatus) {
    val (bg, msg) = when (status) {
        ApiStatus.INVALID -> Color(0xFFFFEBEE) to "API 키 오류 — 설정에서 확인해주세요"
        ApiStatus.UNKNOWN -> Color(0xFFFFFDE7) to "API 키가 설정되지 않았습니다"
        ApiStatus.VALID -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = msg,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF212121)
        )
    }
}

@Composable
private fun StatsHeader(totalCorrections: Int, historyCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "전체 누적 오답: ${totalCorrections}개  ·  분석 횟수: ${historyCount}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "오답 30개마다 자동으로 분석됩니다.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyzeButton(
    apiValid: Boolean,
    isAnalyzing: Boolean,
    hasHistory: Boolean,
    correctionCount: Int,
    onAnalyze: () -> Unit
) {
    val enabled = apiValid && correctionCount > 0 && !isAnalyzing
    val label = when {
        isAnalyzing -> "분석 중..."
        hasHistory -> "지금 다시 분석하기"
        else -> "지금 분석하기"
    }
    Button(
        onClick = onAnalyze,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        if (isAnalyzing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(label)
    }
}

@Composable
private fun AnalyzingPlaceholder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator()
        Text(
            "Claude가 분석 중입니다...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "10~30초 소요됩니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorSurface(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "오류: $message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onDismiss, modifier = Modifier.height(32.dp)) {
                Text("닫기", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AnalysisHistoryCard(
    analysis: WeaknessAnalysis,
    addedSince: Int,
    defaultExpanded: Boolean,
    showGenerateButton: Boolean,
    generateState: GenerateState,
    onGenerate: () -> Unit,
    onResetGenerate: () -> Unit
) {
    var expanded by rememberSaveable(analysis.id) { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        formatDate(analysis.analyzedAt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        buildString {
                            append("분석 이후 +${addedSince}개")
                            analysis.avgScore?.let { append("  ·  평균 ${it}점") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OverallLevelChip(analysis.overallLevel)
                Spacer(Modifier.size(4.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "접기" else "펼치기"
                )
            }

            Text(
                analysis.summary,
                style = MaterialTheme.typography.bodyMedium
            )

            if (expanded) {
                HorizontalDivider()

                if (analysis.weaknessPoints.isNotEmpty()) {
                    SectionHeader("주요 약점")
                    analysis.weaknessPoints.forEach { p -> WeaknessPointItem(p) }
                }

                if (analysis.suggestions.isNotEmpty()) {
                    HorizontalDivider()
                    SectionHeader("개선 방향")
                    analysis.suggestions.forEachIndexed { i, s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${i + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(s, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (analysis.recommendedPatterns.isNotEmpty()) {
                    HorizontalDivider()
                    SectionHeader("추천 연습 패턴")
                    analysis.recommendedPatterns.forEach { p ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                p.pattern,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                p.exampleSentence,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showGenerateButton) {
                    HorizontalDivider()
                    GenerateProblemsSection(
                        recommendedLevel = analysis.recommendedLevel,
                        generateState = generateState,
                        onGenerate = onGenerate,
                        onReset = onResetGenerate
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun OverallLevelChip(level: OverallLevel) {
    val (bg, fg, label) = when (level) {
        OverallLevel.BEGINNER -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "초급")
        OverallLevel.INTERMEDIATE -> Triple(Color(0xFFE3F2FD), Color(0xFF0D47A1), "중급")
        OverallLevel.ADVANCED -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "고급")
    }
    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun WeaknessPointItem(point: WeaknessPoint) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(point.errorType.barColor(), RoundedCornerShape(4.dp))
            )
            Text(
                point.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            SeverityBadge(point.severity)
        }
        Text(
            point.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )
        point.examples.forEach { ex ->
            Text(
                "• $ex",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    val (bg, label) = when (severity) {
        Severity.HIGH -> Color(0xFFFFCDD2) to "높음"
        Severity.MEDIUM -> Color(0xFFFFE0B2) to "중간"
        Severity.LOW -> Color(0xFFC8E6C9) to "낮음"
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GenerateProblemsSection(
    recommendedLevel: Int,
    generateState: GenerateState,
    onGenerate: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("이 약점 기반 문제 만들기")
        Text(
            "분석된 약점을 극복하는 데 초점을 맞춘 새 문제 10개를 생성합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onGenerate,
            enabled = generateState !is GenerateState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (generateState is GenerateState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.size(8.dp))
            }
            Text("Lv.${recommendedLevel} 문제 10개 생성")
        }
        when (generateState) {
            is GenerateState.Success -> Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "레벨 ${generateState.level} 문제 ${generateState.count}개 추가됨",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1B5E20)
                    )
                    OutlinedButton(onClick = onReset, modifier = Modifier.height(28.dp)) {
                        Text("닫기", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            is GenerateState.Error -> Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFEBEE)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "오류: ${generateState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onReset, modifier = Modifier.height(28.dp)) {
                        Text("닫기", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            else -> Unit
        }
    }
}

private fun ErrorType.barColor() = when (this) {
    ErrorType.GRAMMAR -> Color(0xFFEF5350)
    ErrorType.VOCABULARY -> Color(0xFF42A5F5)
    ErrorType.STRUCTURE -> Color(0xFF66BB6A)
    ErrorType.PUNCTUATION -> Color(0xFFFFA726)
    ErrorType.SPELLING -> Color(0xFFAB47BC)
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy년 M월 d일 a h:mm", Locale.KOREAN).format(Date(timestamp))
