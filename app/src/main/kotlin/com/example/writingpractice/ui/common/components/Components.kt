package com.example.writingpractice.ui.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.ui.theme.WpTheme

/** 섹션 구분 제목 — 화면별 중복 정의를 대체 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** 점수 → 색상: 80점 이상 高, 50점 이상 中, 미만 低 */
@Composable
fun scoreColor(score: Int): Color = when {
    score >= 80 -> WpTheme.colors.scoreHigh
    score >= 50 -> WpTheme.colors.scoreMid
    else -> WpTheme.colors.scoreLow
}

/** 점수 표시 칩 */
@Composable
fun ScoreChip(score: Int, modifier: Modifier = Modifier) {
    val color = scoreColor(score)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Text(
            "${score}점",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

enum class BannerKind { Info, Success, Warning, Error }

/** 상태 표시 배너 — 각 화면의 배경색+텍스트 Surface 패턴을 통합 */
@Composable
fun StatusBanner(
    kind: BannerKind,
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val (container, content) = when (kind) {
        BannerKind.Info -> WpTheme.colors.infoContainer to WpTheme.colors.onInfoContainer
        BannerKind.Success -> WpTheme.colors.successContainer to WpTheme.colors.onSuccessContainer
        BannerKind.Warning -> WpTheme.colors.warningContainer to WpTheme.colors.onWarningContainer
        BannerKind.Error -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

/** 오류 유형 → 팔레트 색 */
@Composable
fun errorTypeColor(type: ErrorType): Color = when (type) {
    ErrorType.GRAMMAR -> WpTheme.colors.grammar
    ErrorType.VOCABULARY -> WpTheme.colors.vocabulary
    ErrorType.STRUCTURE -> WpTheme.colors.structure
    ErrorType.PUNCTUATION -> WpTheme.colors.punctuation
    ErrorType.SPELLING -> WpTheme.colors.spelling
}

/** 오류 유형 한글 라벨 */
fun errorTypeLabel(type: ErrorType): String = when (type) {
    ErrorType.GRAMMAR -> "문법"
    ErrorType.VOCABULARY -> "어휘"
    ErrorType.STRUCTURE -> "구조"
    ErrorType.PUNCTUATION -> "구두점"
    ErrorType.SPELLING -> "철자"
}

/** 오류 유형 칩 */
@Composable
fun ErrorTypeChip(type: ErrorType, modifier: Modifier = Modifier) {
    val color = errorTypeColor(type)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Text(
            errorTypeLabel(type),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
