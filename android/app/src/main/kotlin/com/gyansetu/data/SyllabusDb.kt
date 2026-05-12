package com.gyansetu.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "syllabus")
data class SyllabusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,                            // animals, fruits, …
    val en: String,
    val gu: String,
    val phon: String? = null,
    @ColumnInfo(name = "story_en") val storyEn: String? = null,
    @ColumnInfo(name = "story_gu") val storyGu: String? = null,
    val icon: String? = null,
)

@Dao
interface SyllabusDao {
    @Query("SELECT COUNT(*) FROM syllabus") suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<SyllabusEntity>)

    @Query("SELECT * FROM syllabus WHERE topic = :topic")
    suspend fun byTopic(topic: String): List<SyllabusEntity>

    @Query("SELECT * FROM syllabus WHERE en LIKE :q OR gu LIKE :q LIMIT 20")
    suspend fun search(q: String): List<SyllabusEntity>
}

@Database(entities = [SyllabusEntity::class], version = 1, exportSchema = false)
abstract class SyllabusDatabase : RoomDatabase() {
    abstract fun dao(): SyllabusDao

    suspend fun seedIfEmpty() {
        if (dao().count() > 0) return
        dao().insertAll(SeedData.allRows())
    }

    companion object {
        @Volatile private var instance: SyllabusDatabase? = null
        fun get(ctx: Context): SyllabusDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, SyllabusDatabase::class.java, "gyansetu.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
