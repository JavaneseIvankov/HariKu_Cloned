package com.hariku.feature.journal.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.hariku.feature.journal.data.dto.JournalEntryDto
import com.hariku.feature.journal.data.remote.JournalRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface JournalLocalMigrator {
    suspend fun migrateIfNeeded(userId: String)
}

interface LocalJournalStore {
    fun isMigrated(userId: String): Boolean
    fun markMigrated(userId: String)
    fun readEntries(userId: String): List<JournalEntryDto>
    fun deleteEntries(userId: String, ids: List<Long>)
}

/**
 * Migrasi satu kali secara idempoten dari database Room lama (`hariku_journal.db`)
 * ke Cloud Firestore untuk user yang sedang aktif.
 */
class RoomJournalLocalMigrator(
    private val localStore: LocalJournalStore,
    private val remoteDataSource: JournalRemoteDataSource
) : JournalLocalMigrator {

    private val mutex = Mutex()
    private val completedUsers = mutableSetOf<String>()

    override suspend fun migrateIfNeeded(userId: String) {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isEmpty() || normalizedUserId in completedUsers) return

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
            } catch (_: Exception) {
                // Jangan tandai selesai bila gagal agar dapat dicoba ulang di sesi berikutnya.
            }
        }
    }
}

class SqliteLocalJournalStore(
    private val context: Context
) : LocalJournalStore {

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isMigrated(userId: String): Boolean =
        prefs.getBoolean("$KEY_MIGRATED_PREFIX$userId", false)

    override fun markMigrated(userId: String) {
        prefs.edit().putBoolean("$KEY_MIGRATED_PREFIX$userId", true).apply()
    }

    override fun readEntries(userId: String): List<JournalEntryDto> {
        val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return emptyList()

        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            if (!hasTable(db, TABLE_NAME) || !hasColumn(db, TABLE_NAME, COL_USER_ID)) {
                return@use emptyList()
            }

            val entries = mutableListOf<JournalEntryDto>()
            db.rawQuery(
                """
                SELECT id, title, content, dateMillis, tag, templateType, promptUsed, isPinned, updatedAt
                FROM $TABLE_NAME
                WHERE $COL_USER_ID = ?
                """.trimIndent(),
                arrayOf(userId)
            ).use { cursor ->
                val idxId = cursor.getColumnIndexOrThrow("id")
                val idxTitle = cursor.getColumnIndexOrThrow("title")
                val idxContent = cursor.getColumnIndexOrThrow("content")
                val idxDateMillis = cursor.getColumnIndexOrThrow("dateMillis")
                val idxTag = cursor.getColumnIndexOrThrow("tag")
                val idxTemplateType = cursor.getColumnIndexOrThrow("templateType")
                val idxPromptUsed = cursor.getColumnIndexOrThrow("promptUsed")
                val idxIsPinned = cursor.getColumnIndexOrThrow("isPinned")
                val idxUpdatedAt = cursor.getColumnIndexOrThrow("updatedAt")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idxId)
                    if (id <= 0L) continue
                    entries += JournalEntryDto(
                        id = id,
                        userId = userId,
                        title = cursor.getString(idxTitle).orEmpty(),
                        content = cursor.getString(idxContent).orEmpty(),
                        dateMillis = cursor.getLong(idxDateMillis),
                        tag = cursor.getString(idxTag).orEmpty(),
                        templateType = cursor.getString(idxTemplateType).orEmpty(),
                        promptUsed = cursor.getString(idxPromptUsed).orEmpty(),
                        isPinned = cursor.getInt(idxIsPinned) != 0,
                        updatedAt = cursor.getLong(idxUpdatedAt)
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
            if (!hasTable(db, TABLE_NAME) || !hasColumn(db, TABLE_NAME, COL_USER_ID)) return@use
            db.beginTransaction()
            try {
                ids.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { chunk ->
                    val placeholders = chunk.joinToString(",") { "?" }
                    val args = arrayOf(userId) + chunk.map { it.toString() }.toTypedArray()
                    db.execSQL(
                        "DELETE FROM $TABLE_NAME WHERE $COL_USER_ID = ? AND id IN ($placeholders)",
                        args
                    )
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
        private const val DATABASE_NAME = "hariku_journal.db"
        private const val TABLE_NAME = "journal_entries"
        private const val COL_USER_ID = "userId"
        private const val PREFS_NAME = "hariku_journal_migration"
        private const val KEY_MIGRATED_PREFIX = "migrated_"
        private const val SQLITE_BIND_CHUNK_SIZE = 500
    }
}
