package com.example.data.repository

import com.example.data.database.ScoreDao
import com.example.data.database.ScoreRecord
import kotlinx.coroutines.flow.Flow

class GameRepository(private val scoreDao: ScoreDao) {
    val allScores: Flow<List<ScoreRecord>> = scoreDao.getAllScores()

    suspend fun saveScore(score: Int, gameMode: String, starsCount: Int) {
        val record = ScoreRecord(
            score = score,
            gameMode = gameMode,
            starsCount = starsCount
        )
        scoreDao.insertScore(record)
    }

    suspend fun clearHistory() {
        scoreDao.clearAllScores()
    }
}
