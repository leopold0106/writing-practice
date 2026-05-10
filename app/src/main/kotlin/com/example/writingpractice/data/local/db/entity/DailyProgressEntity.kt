package com.example.writingpractice.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_progress")
data class DailyProgressEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "problems_solved") val problemsSolved: Int = 0,
    @ColumnInfo(name = "daily_goal") val dailyGoal: Int,
    @ColumnInfo(name = "level_breakdown") val levelBreakdown: String = "{}"
)
