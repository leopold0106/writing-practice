package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.writingpractice.data.local.db.entity.ProblemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {

    @Query("SELECT * FROM problems WHERE level = :level ORDER BY id ASC")
    fun observeByLevel(level: Int): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE id = :id")
    suspend fun getById(id: Long): ProblemEntity?

    @Query("SELECT * FROM problems WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ProblemEntity?

    @Query("""
        SELECT p.* FROM problems p
        WHERE p.level = :level
        AND p.id NOT IN (
            SELECT ua.problem_id FROM user_answers ua
            WHERE date(ua.submitted_at / 1000, 'unixepoch', 'localtime') = :todayIso
        )
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getNextUnsolved(level: Int, todayIso: String): ProblemEntity?

    @Query("SELECT * FROM problems WHERE level = :level ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByLevel(level: Int): ProblemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(problems: List<ProblemEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(problem: ProblemEntity): Long

    @Query("SELECT COUNT(*) FROM problems WHERE level = :level")
    suspend fun countByLevel(level: Int): Int

    @Query("SELECT COUNT(*) FROM problems WHERE level = :level AND is_prebundled = 1")
    suspend fun countBundled(level: Int): Int

    @Query("""
        SELECT COUNT(*) FROM problems p
        WHERE p.level = :level
        AND p.id NOT IN (
            SELECT ua.problem_id FROM user_answers ua
            WHERE date(ua.submitted_at / 1000, 'unixepoch', 'localtime') = :todayIso
        )
    """)
    suspend fun countUnsolvedForToday(level: Int, todayIso: String): Int
}
