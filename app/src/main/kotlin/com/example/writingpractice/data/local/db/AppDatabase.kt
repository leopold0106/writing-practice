package com.example.writingpractice.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.writingpractice.data.local.db.dao.CorrectionDao
import com.example.writingpractice.data.local.db.dao.ProgressDao
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.local.db.dao.UserAnswerDao
import com.example.writingpractice.data.local.db.dao.WeaknessAnalysisDao
import com.example.writingpractice.data.local.db.entity.CorrectionEntity
import com.example.writingpractice.data.local.db.entity.DailyProgressEntity
import com.example.writingpractice.data.local.db.entity.ErrorType
import com.example.writingpractice.data.local.db.entity.GradingStatus
import com.example.writingpractice.data.local.db.entity.ProblemEntity
import com.example.writingpractice.data.local.db.entity.UserAnswerEntity
import com.example.writingpractice.data.local.db.entity.WeaknessAnalysisEntity

@Database(
    entities = [
        ProblemEntity::class,
        UserAnswerEntity::class,
        CorrectionEntity::class,
        DailyProgressEntity::class,
        WeaknessAnalysisEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun userAnswerDao(): UserAnswerDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun progressDao(): ProgressDao
    abstract fun weaknessAnalysisDao(): WeaknessAnalysisDao

    companion object {
        const val DATABASE_NAME = "writing_practice.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weakness_analyses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        period TEXT NOT NULL,
                        analyzedAt INTEGER NOT NULL,
                        summary TEXT NOT NULL,
                        overallLevel TEXT NOT NULL,
                        weaknessPointsJson TEXT NOT NULL,
                        suggestionsJson TEXT NOT NULL,
                        recommendedPatternsJson TEXT NOT NULL,
                        recommendedLevel INTEGER NOT NULL,
                        totalCorrections INTEGER NOT NULL,
                        avgScore INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
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
