package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.writingpractice.data.local.db.entity.DailyProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyProgressEntity)

    @Query("SELECT * FROM daily_progress WHERE date = :dateIso")
    fun observeForDate(dateIso: String): Flow<DailyProgressEntity?>

    @Query("SELECT * FROM daily_progress WHERE date = :dateIso")
    suspend fun getForDate(dateIso: String): DailyProgressEntity?

    @Query("SELECT * FROM daily_progress ORDER BY date DESC LIMIT 30")
    fun observeRecent(): Flow<List<DailyProgressEntity>>
}
