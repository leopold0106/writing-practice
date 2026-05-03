package com.example.writingpractice.data.model

import com.example.writingpractice.data.local.db.entity.ProblemEntity

data class Problem(
    val id: Long,
    val uuid: String,
    val level: Int,
    val koreanText: String,
    val referenceAnswer: String?,
    val topicTag: String?,
    val isPrebundled: Boolean
)

fun ProblemEntity.toDomain() = Problem(
    id = id,
    uuid = uuid,
    level = level,
    koreanText = koreanText,
    referenceAnswer = referenceAnswer,
    topicTag = topicTag,
    isPrebundled = isPrebundled
)

fun Problem.toEntity() = ProblemEntity(
    id = id,
    uuid = uuid,
    level = level,
    koreanText = koreanText,
    referenceAnswer = referenceAnswer,
    topicTag = topicTag,
    isPrebundled = isPrebundled
)
