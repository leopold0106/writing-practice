package com.example.writingpractice.data.repository

import com.example.writingpractice.data.local.asset.AssetProblemLoader
import com.example.writingpractice.data.local.db.dao.ProblemDao
import com.example.writingpractice.data.model.Problem
import com.example.writingpractice.data.model.toDomain
import com.example.writingpractice.data.model.toEntity
import com.example.writingpractice.util.DateTimeUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProblemRepository @Inject constructor(
    private val problemDao: ProblemDao,
    private val assetLoader: AssetProblemLoader,
    private val settingsRepository: SettingsRepository
) {
    suspend fun seedIfNeeded() {
        if (!settingsRepository.isDbSeeded()) {
            val problems = assetLoader.loadAll()
            if (problems.isNotEmpty()) {
                problemDao.insertAll(problems)
            }
            settingsRepository.setDbSeeded(true)
        }
    }

    fun observeByLevel(level: Int): Flow<List<Problem>> =
        problemDao.observeByLevel(level).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Problem? = problemDao.getById(id)?.toDomain()

    suspend fun getNextProblem(level: Int): Problem? =
        problemDao.getNextUnsolved(level, DateTimeUtil.todayIso())?.toDomain()
            ?: problemDao.getRandomByLevel(level)?.toDomain()

    suspend fun countByLevel(level: Int): Int = problemDao.countByLevel(level)

    suspend fun countUnsolvedForToday(level: Int): Int =
        problemDao.countUnsolvedForToday(level, DateTimeUtil.todayIso())

    suspend fun insertGeneratedProblem(problem: Problem): Long =
        problemDao.insert(problem.toEntity().copy(isPrebundled = false))
}
