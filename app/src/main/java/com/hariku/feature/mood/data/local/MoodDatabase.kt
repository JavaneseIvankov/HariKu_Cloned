package com.hariku.feature.mood.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MoodEntryEntity::class], version = 2, exportSchema = false)
abstract class MoodDatabase : RoomDatabase() {
    abstract fun moodDao(): MoodDao

    companion object {
        private const val DATABASE_NAME = "hariku_mood.db"

        /** Menambah kolom userId; baris lama dipetakan ke scope 'local_guest'. Idempoten. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var hasUserId = false
                db.query("PRAGMA table_info(`mood_entries`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex) == "userId") {
                            hasUserId = true
                            break
                        }
                    }
                }
                if (!hasUserId) {
                    db.execSQL("ALTER TABLE `mood_entries` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'local_guest'")
                }
            }
        }

        fun open(context: Context): MoodDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MoodDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
