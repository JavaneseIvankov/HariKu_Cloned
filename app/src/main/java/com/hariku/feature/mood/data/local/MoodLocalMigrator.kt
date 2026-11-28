package com.hariku.feature.mood.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.hariku.core.auth.LOCAL_GUEST_USER_ID
import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface MoodLocalMigrator {
    suspend fun migrateIfNeeded(userId: String)
}

interface LocalMoodStore {
    fun isMigrated(userId: String): Boolean
    fun markMigrated(userId: String)
    fun readEntries(userId: String): List<MoodEntryDto>
    fun deleteEntries(userId: String, ids: List<Long>)
}

/**
 * Migrasi satu kali secara idempoten dari database Room lama (`hariku_mood.db`)
 * ke Cloud Firestore untuk user yang sedang aktif.
 */
class RoomMoodLocalMigrator(
    private val localStore: LocalMoodStore,
    private val remoteDataSource: MoodRemoteDataSource,
    private val onError: (Throwable) -> Unit = {}
) : MoodLocalMigrator {

    private val mutex = Mutex()
    private val completedUsers = mutableSetOf<String>()

    override suspend fun migrateIfNeeded(userId: String) {
        val normalizedUserId = userId.trim()
        if (
            normalizedUserId.isEmpty() ||
            normalizedUserId == LOCAL_GUEST_USER_ID ||
            normalizedUserId in completedUsers
        ) {
            return
        }

        mutex.withLock {
            if (normalizedUserId in completedUsers) return
            if (localStore.isMigrated(normalizedUserId)) {
                completedUsers.add(normalizedUserId)
                return
            }

            try {
                val legacyEntries = localStore.readEntries(normalizedUserId)
                if (legacyEntries.isNotEmpty()) {
                    remoteDataSource.upsertBatch(legacyEntries, normalizedUserId)
                    localStore.deleteEntries(normalizedUserId, legacyEntries.map { it.id })
                }
                localStore.markMigrated(normalizedUserId)
                completedUsers.add(normalizedUserId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Jangan tandai selesai bila gagal agar dapat dicoba ulang di sesi berikutnya.
                onError(error)
            }
        }
    }
}

class SqliteLocalMoodStore(
    private val context: Context
) : LocalMoodStore {

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isMigrated(userId: String): Boolean =
        prefs.getBoolean("$KEY_MIGRATED_PREFIX$userId", false)

    override fun markMigrated(userId: String) {
        prefs.edit().putBoolean("$KEY_MIGRATED_PREFIX$userId", true).apply()
    }

    override fun readEntries(userId: String): List<MoodEntryDto> {
        val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return emptyList()

        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            if (!hasTable(db, TABLE_NAME)) {
                return@use emptyList()
            }

            val hasUserIdColumn = hasColumn(db, TABLE_NAME, COL_USER_ID)
            val query = if (hasUserIdColumn) {
                """
                SELECT id, moodType, timestamp, note
                FROM $TABLE_NAME
                WHERE $COL_USER_ID = ? OR $COL_USER_ID = ?
                """.trimIndent()
            } else {
                """
                SELECT id, moodType, timestamp, note
                FROM $TABLE_NAME
                """.trimIndent()
            }
            val args = if (hasUserIdColumn) arrayOf(userId, LOCAL_GUEST_USER_ID) else null

            val entries = mutableListOf<MoodEntryDto>()
            db.rawQuery(query, args).use { cursor ->
                val idxId = cursor.getColumnIndexOrThrow("id")
                val idxMoodType = cursor.getColumnIndexOrThrow("moodType")
                val idxTimestamp = cursor.getColumnIndexOrThrow("timestamp")
                val idxNote = cursor.getColumnIndexOrThrow("note")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idxId)
                    if (id <= 0L) continue
                    val timestamp = cursor.getLong(idxTimestamp)
                    entries += MoodEntryDto(
                        id = id,
                        userId = userId,
                        moodType = cursor.getString(idxMoodType).orEmpty(),
                        timestamp = timestamp,
                        note = cursor.getString(idxNote).orEmpty(),
                        updatedAt = timestamp
                    )
                }
            }
            entries
        }
    }

    override fun deleteEntries(userId: String, ids: List<Long>) {
        if (ids.isEmpty()) return
        val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return

        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            if (!hasTable(db, TABLE_NAME)) return@use
            val hasUserIdColumn = hasColumn(db, TABLE_NAME, COL_USER_ID)
            db.beginTransaction()
            try {
                ids.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { chunk ->
                    val placeholders = chunk.joinToString(",") { "?" }
                    if (hasUserIdColumn) {
                        val args = arrayOf(userId, LOCAL_GUEST_USER_ID) +
                            chunk.map { it.toString() }.toTypedArray()
                        db.execSQL(
                            "DELETE FROM $TABLE_NAME WHERE ($COL_USER_ID = ? OR $COL_USER_ID = ?) AND id IN ($placeholders)",
                            args
                        )
                    } else {
                        val args = chunk.map { it.toString() }.toTypedArray()
                        db.execSQL(
                            "DELETE FROM $TABLE_NAME WHERE id IN ($placeholders)",
                            args
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun hasTable(db: SQLiteDatabase, tableName: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tableName)
        ).use { it.moveToFirst() }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean =
        db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                    return@use true
                }
            }
            false
        }

    companion object {
        private const val DATABASE_NAME = "hariku_mood.db"
        private const val TABLE_NAME = "mood_entries"
        private const val COL_USER_ID = "userId"
        private const val PREFS_NAME = "hariku_mood_migration"
        private const val KEY_MIGRATED_PREFIX = "migrated_"
        private const val SQLITE_BIND_CHUNK_SIZE = 500
    }
}
