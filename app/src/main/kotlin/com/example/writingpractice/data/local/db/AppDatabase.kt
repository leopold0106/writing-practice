package com.example.writingpractice.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProgressDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.entity.CorrectionEntity
import com.example.writingpractice.data.local.db.entity.DailyProgressEntity
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.ProblemEntity
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity

@Database(
    entities = [
        ProblemEntity::class,
        UserAnswerEntity::class,
        CorrectionEntity::class,
        DailyProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun userAnswerDao(): UserAnswerDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun progressDao(): ProgressDao

    companion object {
        const val DATABASE_NAME = "writing_practice.db"
    }
}

class Converters {
    @TypeConverter
    fun fromGradingStatus(v: GradingStatus): String = v.name

    @TypeConverter
    fun toGradingStatus(v: String): GradingStatus = GradingStatus.valueOf(v)

    @TypeConverter
    fun fromErrorType(v: ErrorType): String = v.name

    @TypeConverter
    fun toErrorType(v: String): ErrorType = ErrorType.valueOf(v)
}
