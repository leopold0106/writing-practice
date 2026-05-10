package com.example.writingpractice.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.CorrectionEntity
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.ProblemEntity
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import com.example.writingpractice.data.model.AnswerBackup
import com.example.writingpractice.data.model.BackupFile
import com.example.writingpractice.data.model.CorrectionBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val correctionDao: CorrectionDao,
    private val userAnswerDao: UserAnswerDao,
    private val problemDao: ProblemDao,
    private val json: Json
) {
    companion object {
        private const val AUTHORITY = "com.example.writingpractice.fileprovider"
        private const val SUPPORTED_VERSION = 1
    }

    suspend fun exportBackup(): Result<Uri> = try {
        val corrections = correctionDao.getAll()
        val answerCache = mutableMapOf<Long, UserAnswerEntity>()
        val problemCache = mutableMapOf<Long, ProblemEntity>()

        val answers = corrections
            .groupBy { it.userAnswerId }
            .mapNotNull { (answerId, corrs) ->
                val answer = answerCache.getOrPut(answerId) {
                    userAnswerDao.getById(answerId) ?: return@mapNotNull null
                }
                val problem = problemCache.getOrPut(answer.problemId) {
                    problemDao.getById(answer.problemId) ?: return@mapNotNull null
                }
                AnswerBackup(
                    problemUuid = problem.uuid,
                    koreanText = problem.koreanText,
                    referenceAnswer = problem.referenceAnswer,
                    level = problem.level,
                    topicTag = problem.topicTag,
                    isPrebundled = problem.isPrebundled,
                    problemCreatedAt = problem.createdAt,
                    answerText = answer.answerText,
                    score = answer.score,
                    gradingStatus = answer.gradingStatus.name,
                    overallFeedback = answer.overallFeedback,
                    finalCorrectedVersion = answer.finalCorrectedVersion,
                    submittedAt = answer.submittedAt,
                    attemptNumber = answer.attemptNumber,
                    corrections = corrs.map { c ->
                        CorrectionBackup(
                            originalSentence = c.originalSentence,
                            correctedSentence = c.correctedSentence,
                            explanation = c.explanation,
                            errorType = c.errorType.name,
                            isReviewed = c.isReviewed,
                            createdAt = c.createdAt
                        )
                    }
                )
            }

        val backupFile = BackupFile(exportedAt = System.currentTimeMillis(), answers = answers)
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(context.getExternalFilesDir(null), "backup").also { it.mkdirs() }
        val file = File(dir, "writing_practice_backup_$dateStr.json")
        file.writeText(json.encodeToString(backupFile))

        Result.success(FileProvider.getUriForFile(context, AUTHORITY, file))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun importBackup(uri: Uri): Result<Int> = try {
        val jsonStr = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText()
            ?: return Result.failure(Exception("파일을 읽을 수 없습니다"))

        val backup = json.decodeFromString<BackupFile>(jsonStr)
        if (backup.version > SUPPORTED_VERSION) {
            return Result.failure(Exception("지원하지 않는 백업 형식입니다 (버전 ${backup.version})"))
        }

        var imported = 0
        backup.answers.forEach { ab ->
            val existingProblem = problemDao.getByUuid(ab.problemUuid)
            val problemId = existingProblem?.id ?: problemDao.insert(
                ProblemEntity(
                    uuid = ab.problemUuid,
                    level = ab.level,
                    koreanText = ab.koreanText,
                    referenceAnswer = ab.referenceAnswer,
                    topicTag = ab.topicTag,
                    isPrebundled = ab.isPrebundled,
                    createdAt = ab.problemCreatedAt
                )
            )

            val answerId = userAnswerDao.insert(
                UserAnswerEntity(
                    problemId = problemId,
                    answerText = ab.answerText,
                    submittedAt = ab.submittedAt,
                    gradingStatus = runCatching { GradingStatus.valueOf(ab.gradingStatus) }
                        .getOrDefault(GradingStatus.GRADED),
                    score = ab.score,
                    attemptNumber = ab.attemptNumber,
                    overallFeedback = ab.overallFeedback,
                    finalCorrectedVersion = ab.finalCorrectedVersion
                )
            )

            correctionDao.insertAll(ab.corrections.map { c ->
                CorrectionEntity(
                    userAnswerId = answerId,
                    problemId = problemId,
                    originalSentence = c.originalSentence,
                    correctedSentence = c.correctedSentence,
                    explanation = c.explanation,
                    errorType = runCatching { ErrorType.valueOf(c.errorType) }
                        .getOrDefault(ErrorType.GRAMMAR),
                    isReviewed = c.isReviewed,
                    createdAt = c.createdAt
                )
            })
            imported++
        }

        Result.success(imported)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
