package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAnswerDao {

    @Insert
    suspend fun insert(answer: UserAnswerEntity): Long

    @Update
    suspend fun update(answer: UserAnswerEntity)

    @Query("SELECT * FROM user_answers WHERE id = :id")
    suspend fun getById(id: Long): UserAnswerEntity?

    @Query("SELECT * FROM user_answers WHERE id = :id")
    fun observeById(id: Long): Flow<UserAnswerEntity?>

    @Query("SELECT * FROM user_answers WHERE problem_id = :problemId ORDER BY submitted_at DESC")
    fun observeForProblem(problemId: Long): Flow<List<UserAnswerEntity>>

    @Query("SELECT * FROM user_answers WHERE grading_status = 'PENDING'")
    suspend fun getPending(): List<UserAnswerEntity>

    @Query("""
        SELECT COUNT(*) FROM user_answers
        WHERE date(submitted_at / 1000, 'unixepoch', 'localtime') = :dateIso
        AND grading_status = 'GRADED'
    """)
    fun observeCountForDate(dateIso: String): Flow<Int>

    @Query("""
        SELECT ua.* FROM user_answers ua
        JOIN problems p ON ua.problem_id = p.id
        WHERE p.level = :level
        ORDER BY ua.submitted_at DESC
    """)
    fun observeAllForLevel(level: Int): Flow<List<UserAnswerEntity>>

    @Query("""
        SELECT COUNT(*) FROM user_answers
        WHERE problem_id = :problemId AND grading_status = 'GRADED'
    """)
    suspend fun countGradedForProblem(problemId: Long): Int

    @Query("SELECT score FROM user_answers WHERE problem_id = :problemId AND grading_status = 'GRADED' ORDER BY submitted_at DESC LIMIT 1")
    suspend fun getLatestScoreForProblem(problemId: Long): Int?
}
