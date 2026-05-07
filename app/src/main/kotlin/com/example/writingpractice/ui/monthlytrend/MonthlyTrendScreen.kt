package com.example.writingpractice.ui.monthlytrend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.writingpractice.data.model.ErrorTrend
import com.example.writingpractice.data.model.ErrorTypeChange
import com.example.writingpractice.data.model.MonthlySnapshot
import com.example.writingpractice.data.model.MonthlyTrend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyTrendScreen(
    onBack: () -> Unit,
    viewModel: MonthlyTrendViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("월별 학습 추이") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (state.snapshots.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "두 달 이상 사용 후 첫 오픈 시 자동으로 분석됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.snapshots, key = { it.yearMonth }) { snapshot ->
                MonthlySnapshotCard(snapshot)
            }
        }
    }
}

@Composable
private fun MonthlySnapshotCard(snapshot: MonthlySnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatYearMonth(snapshot.yearMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TrendChip(snapshot.overallTrend)
            }

            Text(
                snapshot.comparisonSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val scoreText = buildString {
                snapshot.previousMonthAvgScore?.let { prev ->
                    snapshot.currentMonthAvgScore?.let { curr ->
                        append("평균 점수: ${prev}점 → ${curr}점")
                    }
                }
            }
            if (scoreText.isNotEmpty()) {
                Text(scoreText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            if (snapshot.errorChanges.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "오류 유형 변화",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                snapshot.errorChanges.forEach { change ->
                    ErrorChangeRow(change)
                }
            }

            if (snapshot.keyImprovements.isNotEmpty()) {
                HorizontalDivider()
                Text("잘한 점", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                snapshot.keyImprovements.forEach { item ->
                    Text("✓ $item", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (snapshot.areasToFocus.isNotEmpty()) {
                HorizontalDivider()
                Text("집중 필요", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                snapshot.areasToFocus.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TrendChip(trend: MonthlyTrend) {
    val (label, color) = when (trend) {
        MonthlyTrend.IMPROVING -> "개선 중" to Color(0xFF2E7D32)
        MonthlyTrend.DECLINING -> "주의 필요" to Color(0xFFC62828)
        MonthlyTrend.STABLE -> "유지 중" to Color(0xFF546E7A)
        MonthlyTrend.MIXED -> "혼합" to Color(0xFFE65100)
    }
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorChangeRow(change: ErrorTypeChange) {
    val (arrow, arrowColor) = when (change.trend) {
        ErrorTrend.IMPROVED -> "↓" to Color(0xFF2E7D32)
        ErrorTrend.WORSENED -> "↑" to Color(0xFFC62828)
        ErrorTrend.STABLE -> "→" to Color(0xFF546E7A)
    }
    val errorTypeLabel = when (change.errorType.name) {
        "GRAMMAR" -> "문법"
        "VOCABULARY" -> "어휘"
        "STRUCTURE" -> "구조"
        "PUNCTUATION" -> "구두점"
        "SPELLING" -> "철자"
        else -> change.errorType.name
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            errorTypeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(48.dp)
        )
        Text(
            "${change.previousCount}→${change.currentCount} $arrow",
            style = MaterialTheme.typography.labelSmall,
            color = arrowColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            change.insight,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatYearMonth(ym: String): String {
    val parts = ym.split("-")
    return if (parts.size == 2) "${parts[0]}년 ${parts[1].trimStart('0')}월" else ym
}
