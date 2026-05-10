package com.example.writingpractice.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weakness_analyses")
data class WeaknessAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val analyzedAt: Long = System.currentTimeMillis(),
    val summary: String,
    val overallLevel: String,
    val weaknessPointsJson: String,
    val suggestionsJson: String,
    val recommendedPatternsJson: String,
    val recommendedLevel: Int,
    val totalCorrections: Int,
    val avgScore: Int?
)
