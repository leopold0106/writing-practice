package com.example.writingpractice.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GradingStatus { PENDING, GRADED, OFFLINE_SKIPPED }

@Entity(
    tableName = "user_answers",
    foreignKeys = [ForeignKey(
        entity = ProblemEntity::class,
        parentColumns = ["id"],
        childColumns = ["problem_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("problem_id")]
)
data class UserAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "problem_id") val problemId: Long,
    @ColumnInfo(name = "answer_text") val answerText: String,
    @ColumnInfo(name = "submitted_at") val submittedAt: Long,
    @ColumnInfo(name = "grading_status") val gradingStatus: GradingStatus = GradingStatus.PENDING,
    @ColumnInfo(name = "score") val score: Int? = null,
    @ColumnInfo(name = "attempt_number") val attemptNumber: Int = 1,
    @ColumnInfo(name = "overall_feedback") val overallFeedback: String? = null,
    @ColumnInfo(name = "final_corrected_version") val finalCorrectedVersion: String? = null
)
