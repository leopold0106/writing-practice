package com.example.writingpractice.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ErrorType { GRAMMAR, VOCABULARY, STRUCTURE, PUNCTUATION, SPELLING }

@Entity(
    tableName = "corrections",
    foreignKeys = [
        ForeignKey(
            entity = UserAnswerEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_answer_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProblemEntity::class,
            parentColumns = ["id"],
            childColumns = ["problem_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_answer_id"), Index("problem_id")]
)
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_answer_id") val userAnswerId: Long,
    @ColumnInfo(name = "problem_id") val problemId: Long,
    @ColumnInfo(name = "original_sentence") val originalSentence: String,
    @ColumnInfo(name = "corrected_sentence") val correctedSentence: String,
    @ColumnInfo(name = "explanation") val explanation: String,
    @ColumnInfo(name = "error_type") val errorType: ErrorType,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_reviewed") val isReviewed: Boolean = false
)
