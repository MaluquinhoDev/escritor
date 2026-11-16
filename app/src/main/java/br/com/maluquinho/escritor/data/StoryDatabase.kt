package br.com.maluquinho.escritor.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class StoryProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val format: String = "Romance",
    val synopsis: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = StoryProject::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class StoryChapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val content: String = "",
    val position: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StoryProject>>

    @Insert
    suspend fun insert(project: StoryProject): Long

    @Update
    suspend fun update(project: StoryProject)

    @Delete
    suspend fun delete(project: StoryProject)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY position, id")
    fun observeForProject(projectId: Long): Flow<List<StoryChapter>>

    @Insert
    suspend fun insert(chapter: StoryChapter): Long

    @Update
    suspend fun update(chapter: StoryChapter)

    @Delete
    suspend fun delete(chapter: StoryChapter)
}

@Database(entities = [StoryProject::class, StoryChapter::class], version = 1, exportSchema = false)
abstract class StoryDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao
    abstract fun chapters(): ChapterDao

    companion object {
        @Volatile private var INSTANCE: StoryDatabase? = null

        fun get(context: Context): StoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StoryDatabase::class.java,
                    "escritor.db"
                ).build().also { INSTANCE = it }
            }
    }
}
