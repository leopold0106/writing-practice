package com.example.writingpractice.ui.common

enum class Period(val label: String, val days: Long?) {
    WEEK("1주", 7L),
    MONTH("1개월", 30L),
    THREE_MONTHS("3개월", 90L),
    SIX_MONTHS("6개월", 180L),
    YEAR("1년", 365L),
    ALL("전체", null)
}

val Period.sinceMs: Long
    get() = days?.let { System.currentTimeMillis() - it * 24L * 60 * 60 * 1000 } ?: 0L
