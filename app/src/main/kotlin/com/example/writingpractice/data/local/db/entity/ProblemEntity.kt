package com.example.writingpractice.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "problems",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "korean_text") val koreanText: String,
    @ColumnInfo(name = "reference_answer") val referenceAnswer: String?,
    @ColumnInfo(name = "topic_tag") val topicTag: String?,
    @ColumnInfo(name = "is_prebundled") val isPrebundled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
