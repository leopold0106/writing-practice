package com.example.writingpractice.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_snapshots")
data class MonthlySnapshotEntity(
    @PrimaryKey val yearMonth: String,
    val analyzedAt: Long,
    val comparisonSummary: String,
    val overallTrend: String,
    val errorChangesJson: String,
    val keyImprovementsJson: String,
    val areasToFocusJson: String,
    val currentMonthCorrections: Int,
    val previousMonthCorrections: Int,
    val currentMonthAvgScore: Int?,
    val previousMonthAvgScore: Int?
)
