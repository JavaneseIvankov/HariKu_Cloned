package com.hariku.feature.mood

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mood_entries")
data class MoodEntry(
    val moodType: String,
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntry)

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC, id DESC")
    fun observeEntries(): Flow<List<MoodEntry>>
}

@Database(entities = [MoodEntry::class], version = 1, exportSchema = false)
abstract class MoodDatabase : RoomDatabase() {
    abstract fun moodDao(): MoodDao

    companion object {
        fun open(context: Context, userId: String): MoodDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MoodDatabase::class.java,
                "mood-analytics-${Uri.encode(userId)}.db"
            ).build()
    }
}
