package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.writingpractice.data.local.db.entity.CorrectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrectionDao {

    @Insert
    suspend fun insertAll(corrections: List<CorrectionEntity>)

    @Query("SELECT * FROM corrections WHERE user_answer_id = :answerId")
    fun observeForAnswer(answerId: Long): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE problem_id = :problemId ORDER BY created_at DESC")
    fun observeForProblem(problemId: Long): Flow<List<CorrectionEntity>>

    @Query("""
        SELECT c.* FROM corrections c
        INNER JOIN user_answers ua ON c.user_answer_id = ua.id
        ORDER BY ua.submitted_at DESC
    """)
    fun observeAll(): Flow<List<CorrectionEntity>>

    @Query("""
        SELECT c.* FROM corrections c
        INNER JOIN user_answers ua ON c.user_answer_id = ua.id
        WHERE c.error_type = :errorType
        ORDER BY ua.submitted_at DESC
    """)
    fun observeByErrorType(errorType: String): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE is_reviewed = 0 ORDER BY created_at DESC")
    fun observeUnreviewed(): Flow<List<CorrectionEntity>>

    @Query("UPDATE corrections SET is_reviewed = 1 WHERE problem_id = :problemId")
    suspend fun markReviewed(problemId: Long)

    @Query("SELECT COUNT(*) FROM corrections WHERE is_reviewed = 0")
    fun observeUnreviewedCount(): Flow<Int>

    @Query("SELECT DISTINCT problem_id FROM corrections ORDER BY created_at DESC")
    fun observeDistinctProblemIds(): Flow<List<Long>>

    @Query("SELECT error_type FROM corrections GROUP BY error_type ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun getMostCommonErrorTypes(limit: Int): List<String>

    @Query("SELECT * FROM corrections WHERE created_at >= :sinceMs")
    fun observeCorrectionsAfter(sinceMs: Long): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE created_at >= :sinceMs ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentCorrections(sinceMs: Long, limit: Int): List<CorrectionEntity>

    @Query("SELECT COUNT(*) FROM corrections WHERE created_at >= :sinceMs")
    suspend fun countCorrectionsAfter(sinceMs: Long): Int
}
