package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScoreRecord::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract val scoreDao: ScoreDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "tap_master_kids_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
