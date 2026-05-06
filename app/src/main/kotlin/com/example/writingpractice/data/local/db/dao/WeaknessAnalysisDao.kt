package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.writingpractice.data.local.db.entity.WeaknessAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeaknessAnalysisDao {

    @Insert
    suspend fun insert(entity: WeaknessAnalysisEntity): Long

    @Query("SELECT * FROM weakness_analyses WHERE period = :period ORDER BY analyzedAt DESC LIMIT 1")
    fun observeLatestByPeriod(period: String): Flow<WeaknessAnalysisEntity?>

    @Query("SELECT * FROM weakness_analyses ORDER BY analyzedAt DESC")
    fun observeAll(): Flow<List<WeaknessAnalysisEntity>>

    @Query("SELECT * FROM weakness_analyses WHERE id = :id")
    suspend fun getById(id: Long): WeaknessAnalysisEntity?
}
