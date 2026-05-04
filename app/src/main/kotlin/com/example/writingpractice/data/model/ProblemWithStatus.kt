package com.example.writingpractice.data.model

import com.example.writingpractice.data.local.db.entity.GradingStatus

data class ProblemWithStatus(
    val problem: Problem,
    val latestAnswerId: Long?,
    val latestStatus: GradingStatus?,
    val latestScore: Int?,
    val attemptCount: Int
) {
    val isNew: Boolean get() = latestAnswerId == null
    val isPending: Boolean get() = latestStatus == GradingStatus.PENDING
    val isGraded: Boolean get() = latestStatus == GradingStatus.GRADED
}
