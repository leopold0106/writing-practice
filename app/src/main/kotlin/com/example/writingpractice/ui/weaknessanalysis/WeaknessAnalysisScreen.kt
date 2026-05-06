package com.example.writingpractice.ui.weaknessanalysis

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
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
import com.example.writingpractice.ui.common.Period
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.apiStatus != ApiStatus.VALID) {
                item { ApiStatusBanner(state.apiStatus) }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Period.entries) { period ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            item {
                StatsRow(
                    correctionCount = state.correctionCount,
                    latestAnalysis = state.latestAnalysis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                AnalyzeButton(
                    state = state,
                    onAnalyze = viewModel::analyze,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            state.error?.let { errorMsg ->
                item {
                    ErrorSurface(
                        message = errorMsg,
                        onDismiss = viewModel::dismissError,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (state.isAnalyzing) {
                item { AnalyzingPlaceholder() }
            }

            val analysis = state.latestAnalysis
            if (analysis != null && !state.isAnalyzing) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { SummaryCard(analysis, Modifier.padding(horizontal = 16.dp)) }
                item { WeaknessPointsCard(analysis.weaknessPoints, Modifier.padding(horizontal = 16.dp)) }
                if (analysis.suggestions.isNotEmpty()) {
                    item { SuggestionsCard(analysis.suggestions, Modifier.padding(horizontal = 16.dp)) }
                }
                if (analysis.recommendedPatterns.isNotEmpty()) {
                    item {
                        PatternsCard(analysis.recommendedPatterns, Modifier.padding(horizontal = 16.dp))
                    }
                }
                item {
                    GenerateProblemsCard(
                        recommendedLevel = analysis.recommendedLevel,
                        generateState = generateState,
                        onGenerate = viewModel::generateProblemsFromWeaknesses,
                        onReset = viewModel::resetGenerateState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else if (analysis == null && !state.isAnalyzing && state.correctionCount > 0) {
                item {
                    EmptyHint(
                        text = "“분석 시작”을 눌러 최근 오답 패턴을 진단해 보세요.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            } else if (analysis == null && !state.isAnalyzing && state.correctionCount == 0) {
                item {
                    EmptyHint(
                        text = "이 기간에 분석할 오답이 없습니다.\n먼저 문제를 풀어보세요.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
private fun StatsRow(
    correctionCount: Int,
    latestAnalysis: WeaknessAnalysis?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "분석 대상 오답: ${correctionCount}개",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (latestAnalysis != null) {
            Text(
                "최근 분석: ${formatRelative(latestAnalysis.analyzedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyzeButton(
    state: WeaknessAnalysisUiState,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = state.apiStatus == ApiStatus.VALID &&
            state.correctionCount > 0 &&
            !state.isAnalyzing
    val label = when {
        state.isAnalyzing -> "분석 중..."
        state.latestAnalysis != null -> "다시 분석"
        else -> "분석 시작"
    }
    Button(
        onClick = onAnalyze,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp)
    ) {
        if (state.isAnalyzing) {
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
        modifier = Modifier.fillMaxWidth().padding(32.dp),
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
private fun ErrorSurface(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SummaryCard(analysis: WeaknessAnalysis, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("종합 평가", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OverallLevelChip(analysis.overallLevel)
            }
            Text(analysis.summary, style = MaterialTheme.typography.bodyMedium)
            if (analysis.avgScore != null) {
                Text(
                    "평균 점수: ${analysis.avgScore}점",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
private fun WeaknessPointsCard(points: List<WeaknessPoint>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("주요 약점", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (points.isEmpty()) {
                Text(
                    "뚜렷한 약점 패턴이 발견되지 않았습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                points.forEach { p -> WeaknessPointItem(p) }
            }
        }
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
private fun SuggestionsCard(suggestions: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("개선 방향", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            suggestions.forEachIndexed { i, s ->
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
    }
}

@Composable
private fun PatternsCard(patterns: List<RecommendedPattern>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("추천 연습 패턴", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            patterns.forEach { p ->
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
    }
}

@Composable
private fun GenerateProblemsCard(
    recommendedLevel: Int,
    generateState: GenerateState,
    onGenerate: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "이 약점 기반 문제 만들기",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
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
}

private fun ErrorType.barColor() = when (this) {
    ErrorType.GRAMMAR -> Color(0xFFEF5350)
    ErrorType.VOCABULARY -> Color(0xFF42A5F5)
    ErrorType.STRUCTURE -> Color(0xFF66BB6A)
    ErrorType.PUNCTUATION -> Color(0xFFFFA726)
    ErrorType.SPELLING -> Color(0xFFAB47BC)
}

private fun formatRelative(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> SimpleDateFormat("yyyy.M.d", Locale.KOREAN).format(Date(timestamp))
    }
}
