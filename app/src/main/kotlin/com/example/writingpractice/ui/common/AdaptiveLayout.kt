package com.example.writingpractice.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** MainActivity에서 계산해 주입하는 현재 창 폭 클래스 */
val LocalWindowWidthSizeClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

/** 현재 창이 Medium 이상(태블릿/폴더블 펼침)인지 */
val isExpandedWidth: Boolean
    @Composable get() = LocalWindowWidthSizeClass.current != WindowWidthSizeClass.Compact

/**
 * 스크롤형 화면의 공통 컨테이너.
 * 태블릿에서 콘텐츠를 [maxWidth]로 제한하고 중앙 정렬한다.
 * Scaffold의 padding은 바깥 Box에 적용해 배경이 전폭으로 이어지게 한다.
 */
@Composable
fun AdaptiveContentColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 720.dp,
    scrollable: Boolean = true,
    innerPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        val columnModifier = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
            .padding(innerPadding)
        Column(
            modifier = columnModifier,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * LazyColumn형 화면의 공통 컨테이너.
 * 태블릿에서 리스트 폭을 [maxWidth]로 제한하고 중앙 정렬한다.
 */
@Composable
fun AdaptiveLazyColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 720.dp,
    listContentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    state: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            state = state,
            reverseLayout = reverseLayout,
            contentPadding = listContentPadding,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}
