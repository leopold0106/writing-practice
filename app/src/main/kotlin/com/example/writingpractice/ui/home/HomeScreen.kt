package com.example.writingpractice.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.writingpractice.ui.common.AdaptiveContentColumn
import com.example.writingpractice.ui.common.isExpandedWidth
import com.example.writingpractice.ui.theme.WpTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLevelClick: (Int) -> Unit,
    onNotebookClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWeaknessAnalysisClick: () -> Unit,
    onMonthlyTrendClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val generateState by viewModel.generateState.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isMonthlyAnalysisRunning by viewModel.isMonthlyAnalysisRunning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("영작 연습", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(
                        badge = {
                            if (state.unreviewedCorrections > 0) {
                                Badge { Text(state.unreviewedCorrections.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotebookClick) {
                            Icon(Icons.Default.MenuBook, contentDescription = "오답노트")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        }
    ) { padding ->
        val expanded = isExpandedWidth
        AdaptiveContentColumn(
            contentPadding = padding,
            maxWidth = if (expanded) 840.dp else 640.dp
        ) {
            updateInfo?.let { info ->
                UpdateBanner(
                    version = info.version,
                    onUpdate = onSettingsClick,
                    onDismiss = viewModel::dismissUpdate
                )
            }

            ApiStatusCard(apiStatus = state.apiStatus)

            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) {
                        StreakCard(streak = state.currentStreak)
                    }
                    Box(Modifier.weight(1f)) {
                        DailyProgressCard(solved = state.todaySolved, goal = state.dailyGoal)
                    }
                }
            } else {
                StreakCard(streak = state.currentStreak)
                DailyProgressCard(solved = state.todaySolved, goal = state.dailyGoal)
            }

            Text(
                text = "레벨 선택",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val levelDescriptions = listOf(
                "짧고 간단한 단문",
                "수식어 포함 단문",
                "접속사로 연결된 2~3문장",
                "종속절 포함 복합 단문",
                "고급 문법·형식 문체",
                "어려운 2~3문장",
                "학술 수준 문단"
            )
            val levelColors = WpTheme.colors.levelColors
            if (expanded) {
                // 태블릿: 2열 그리드 (verticalScroll 안이므로 non-lazy Row 사용)
                levelDescriptions.withIndex().chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { (index, desc) ->
                            Box(Modifier.weight(1f)) {
                                LevelButton(
                                    level = index + 1,
                                    description = desc,
                                    color = levelColors[index],
                                    onClick = { onLevelClick(index + 1) }
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                levelDescriptions.forEachIndexed { index, desc ->
                    val level = index + 1
                    LevelButton(
                        level = level,
                        description = desc,
                        color = levelColors[index],
                        onClick = { onLevelClick(level) }
                    )
                }
            }

            HorizontalDivider()

            WeaknessAnalysisCard(onClick = onWeaknessAnalysisClick)

            MonthlyTrendCard(
                isRunning = isMonthlyAnalysisRunning,
                onClick = onMonthlyTrendClick
            )

            GenerateSection(
                generateState = generateState,
                onGenerate = viewModel::generateProblem,
                onReset = viewModel::resetGenerateState
            )
        }
    }
}

@Composable
private fun UpdateBanner(version: String, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = WpTheme.colors.infoContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "새 버전 $version 사용 가능",
                style = MaterialTheme.typography.bodySmall,
                color = WpTheme.colors.onInfoContainer,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onUpdate,
                modifier = Modifier.height(32.dp)
            ) {
                Text("업데이트", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = WpTheme.colors.onInfoContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ApiStatusCard(apiStatus: ApiStatus) {
    val (backgroundColor, contentColor, message) = when (apiStatus) {
        ApiStatus.VALID -> Triple(
            WpTheme.colors.successContainer,
            WpTheme.colors.onSuccessContainer,
            "API 연결됨 ✓"
        )
        ApiStatus.INVALID -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "API 키 오류 — 설정에서 확인해주세요"
        )
        ApiStatus.UNKNOWN -> Triple(
            WpTheme.colors.infoContainer,
            WpTheme.colors.onInfoContainer,
            "API 키가 설정되지 않았습니다"
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
    }
}

@Composable
private fun StreakCard(streak: Int) {
    val (bgColor, message) = when {
        streak >= 30 -> MaterialTheme.colorScheme.secondaryContainer to "전설의 학습자!"
        streak >= 14 -> MaterialTheme.colorScheme.secondaryContainer to "2주 연속 달성!"
        streak >= 7 -> WpTheme.colors.warningContainer to "한 주를 완주했어요!"
        streak >= 3 -> WpTheme.colors.warningContainer to "연속 학습 중!"
        streak == 1 -> MaterialTheme.colorScheme.surfaceVariant to "첫 걸음을 내딛었어요!"
        else -> MaterialTheme.colorScheme.surfaceVariant to "오늘 첫 문제를 풀어보세요"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (streak > 0) "🔥" else "💤",
                    style = MaterialTheme.typography.titleLarge
                )
                Column {
                    Text(
                        text = if (streak > 0) "${streak}일 연속" else "0일",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyProgressCard(solved: Int, goal: Int) {
    val progress = if (goal > 0) (solved.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("오늘의 진행도", style = MaterialTheme.typography.titleSmall)
                Text(
                    "$solved / $goal 문제",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            if (solved >= goal && goal > 0) {
                Text(
                    "오늘 목표를 달성했어요!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun LevelButton(
    level: Int,
    description: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("레벨 $level", fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WeaknessAnalysisCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "AI 약점 분석",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "최근 오답 패턴을 진단하고 개선 방향을 제안받으세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("분석 시작")
            }
        }
    }
}

@Composable
private fun MonthlyTrendCard(isRunning: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "월별 학습 추이",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "매달 오류 패턴 변화를 자동으로 분석합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "월별 추이 분석 중...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("월별 학습 추이 보기")
                }
            }
        }
    }
}

@Composable
private fun GenerateSection(
    generateState: GenerateState,
    onGenerate: (Int) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "AI 문제 생성",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Claude API를 사용해 새 문제를 만듭니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val loadingLevel = (generateState as? GenerateState.Loading)?.level

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..7).forEach { level ->
                OutlinedButton(
                    onClick = { onGenerate(level) },
                    enabled = generateState !is GenerateState.Loading,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    if (loadingLevel == level) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("L$level", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        when (generateState) {
            is GenerateState.Success -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = WpTheme.colors.successContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "레벨 ${generateState.level} 문제 ${generateState.count}개가 추가되었습니다!",
                            style = MaterialTheme.typography.bodySmall,
                            color = WpTheme.colors.onSuccessContainer
                        )
                        OutlinedButton(onClick = onReset, modifier = Modifier.height(32.dp)) {
                            Text("닫기", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            is GenerateState.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "오류: ${generateState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onReset, modifier = Modifier.height(32.dp)) {
                            Text("닫기", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
