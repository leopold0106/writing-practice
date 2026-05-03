package com.example.writingpractice.data.local.asset

import android.content.Context
import com.example.writingpractice.data.local.db.entity.ProblemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProblemAssetDto(
    val uuid: String,
    val level: Int,
    @SerialName("korean_text") val koreanText: String,
    @SerialName("reference_answer") val referenceAnswer: String? = null,
    @SerialName("topic_tag") val topicTag: String? = null
) {
    fun toEntity() = ProblemEntity(
        uuid = uuid,
        level = level,
        koreanText = koreanText,
        referenceAnswer = referenceAnswer,
        topicTag = topicTag,
        isPrebundled = true
    )
}

@Singleton
class AssetProblemLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val fileNames = listOf(
        "problems/level1_problems.json",
        "problems/level2_problems.json",
        "problems/level3_problems.json",
        "problems/level4_problems.json"
    )

    suspend fun loadAll(): List<ProblemEntity> = withContext(Dispatchers.IO) {
        fileNames.flatMap { fileName ->
            try {
                val raw = context.assets.open(fileName).bufferedReader().readText()
                json.decodeFromString<List<ProblemAssetDto>>(raw).map { it.toEntity() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
