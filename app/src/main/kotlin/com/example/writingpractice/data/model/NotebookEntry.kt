package com.example.writingpractice.data.model

data class NotebookEntry(
    val problemId: Long,
    val koreanText: String,
    val level: Int,
    val corrections: List<Correction>
)
