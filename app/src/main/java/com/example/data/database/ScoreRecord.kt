package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val gameMode: String, // "classic" or "color_match"
    val starsCount: Int,  // 0 to 3 stars based on performance
    val timestamp: Long = System.currentTimeMillis()
)
