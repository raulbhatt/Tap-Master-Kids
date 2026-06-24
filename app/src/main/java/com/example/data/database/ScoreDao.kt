package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY timestamp DESC")
    fun getAllScores(): Flow<List<ScoreRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(scoreRecord: ScoreRecord)

    @Query("DELETE FROM scores")
    suspend fun clearAllScores()
}
