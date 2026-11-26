package com.hariku.feature.journal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JournalEntryEntity::class], version = 2, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        private const val DATABASE_NAME = "hariku_journal.db"

        /** Menambah kolom userId; baris lama dipetakan ke scope 'local_guest'. Idempoten. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var hasUserId = false
                db.query("PRAGMA table_info(`journal_entries`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex) == "userId") {
                            hasUserId = true
                            break
                        }
                    }
                }
                if (!hasUserId) {
                    db.execSQL("ALTER TABLE `journal_entries` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'local_guest'")
                }
            }
        }

        fun open(context: Context): JournalDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
