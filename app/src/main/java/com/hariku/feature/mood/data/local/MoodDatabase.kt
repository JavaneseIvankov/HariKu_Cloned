package com.hariku.feature.mood.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MoodEntryEntity::class], version = 1, exportSchema = false)
abstract class MoodDatabase : RoomDatabase() {
    abstract fun moodDao(): MoodDao

    companion object {
        private const val DATABASE_NAME = "hariku_mood.db"

        fun open(context: Context): MoodDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MoodDatabase::class.java,
                DATABASE_NAME
            ).build()
    }
}
