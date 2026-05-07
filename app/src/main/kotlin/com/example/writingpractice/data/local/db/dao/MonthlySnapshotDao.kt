package com.example.writingpractice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.writingpractice.data.local.db.entity.MonthlySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MonthlySnapshotEntity)

    @Query("SELECT * FROM monthly_snapshots ORDER BY yearMonth DESC")
    fun observeAll(): Flow<List<MonthlySnapshotEntity>>
}
