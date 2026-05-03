package com.example.writingpractice.data.model

data class ProblemWithStatus(
    val problem: Problem,
    val latestScore: Int?,
    val attemptCount: Int
) {
    val isNew: Boolean get() = attemptCount == 0
    val isAttempted: Boolean get() = attemptCount > 0 && latestScore == null
    val isGraded: Boolean get() = latestScore != null
}
