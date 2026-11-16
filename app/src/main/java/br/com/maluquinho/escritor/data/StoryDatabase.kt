package br.com.maluquinho.escritor.data
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class StoryProject(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val format: String = "Romance", val synopsis: String = "", val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "chapters", foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class StoryChapter(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val title: String,val content: String = "",val position: Int = 0,val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "scenes", foreignKeys = [ForeignKey(entity = StoryChapter::class,parentColumns = ["id"],childColumns = ["chapterId"],onDelete = ForeignKey.CASCADE)],indices = [Index("chapterId")])
data class StoryScene(@PrimaryKey(autoGenerate = true) val id: Long = 0,val chapterId: Long,val title: String,val summary: String = "",val goal: String = "",val conflict: String = "",val outcome: String = "",val content: String = "",val position: Int = 0,val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "characters",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class StoryCharacter(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val name: String,val role: String = "",val description: String = "",val motivation: String = "",val goal: String = "",val need: String = "",val flaw: String = "",val arc: String = "")
@Entity(tableName = "places",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class StoryPlace(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val name: String,val description: String = "",val rules: String = "",val atmosphere: String = "")
@Entity(tableName = "world_entries",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class WorldEntry(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val kind: String = "Objeto",val name: String,val description: String = "")
@Entity(tableName = "relationships",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class CharacterRelationship(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val fromCharacter: String,val toCharacter: String,val description: String = "",val evolution: String = "")
@Entity(tableName = "timeline_events",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class TimelineEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val title: String,val storyDate: String = "",val description: String = "",val consequences: String = "",val orderIndex: Int = 0)
@Entity(tableName = "story_snapshots",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class StorySnapshot(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val chapterId: Long? = null,val title: String,val content: String,val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "story_notes",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class StoryNote(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val title: String,val content: String = "",val category: String = "Pesquisa",val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "writing_goals",foreignKeys = [ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE)],indices = [Index("projectId")])
data class WritingGoal(@PrimaryKey(autoGenerate = true) val id: Long = 0,val projectId: Long,val targetWords: Int = 500,val currentWords: Int = 0,val targetDate: String = "")
@Entity(tableName = "app_settings")
data class AppSetting(@PrimaryKey val key: String,val value: String)

@Entity(
 tableName = "narrative_links",
 foreignKeys = [
  ForeignKey(entity = StoryProject::class,parentColumns = ["id"],childColumns = ["projectId"],onDelete = ForeignKey.CASCADE),
  ForeignKey(entity = StoryChapter::class,parentColumns = ["id"],childColumns = ["chapterId"],onDelete = ForeignKey.CASCADE)
 ],
 indices = [Index("projectId"),Index("chapterId"),Index(value = ["chapterId","elementType","elementId"],unique = true)]
)
data class NarrativeLink(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val projectId: Long,
 val chapterId: Long,
 val elementType: String,
 val elementId: Long,
 val createdAt: Long = System.currentTimeMillis()
)

@Dao interface ProjectDao {
 @Query("SELECT * FROM projects ORDER BY createdAt DESC") fun observeAll(): Flow<List<StoryProject>>
 @Insert suspend fun insert(project: StoryProject): Long
 @Update suspend fun update(project: StoryProject)
 @Delete suspend fun delete(project: StoryProject)
}
@Dao interface ChapterDao {
 @Query("SELECT * FROM chapters WHERE projectId=:projectId ORDER BY position,id") fun observeForProject(projectId:Long):Flow<List<StoryChapter>>
 @Insert suspend fun insert(chapter:StoryChapter):Long
 @Update suspend fun update(chapter:StoryChapter)
 @Delete suspend fun delete(chapter:StoryChapter)
}
@Dao interface SceneDao {
 @Query("SELECT * FROM scenes WHERE chapterId=:chapterId ORDER BY position,id") fun observeForChapter(chapterId:Long):Flow<List<StoryScene>>
 @Insert suspend fun insert(item:StoryScene):Long
 @Update suspend fun update(item:StoryScene)
 @Delete suspend fun delete(item:StoryScene)
}
@Dao interface CharacterDao {
 @Query("SELECT * FROM characters WHERE projectId=:projectId ORDER BY name COLLATE NOCASE") fun observe(projectId:Long):Flow<List<StoryCharacter>>
 @Insert suspend fun insert(item:StoryCharacter):Long
 @Update suspend fun update(item:StoryCharacter)
 @Delete suspend fun delete(item:StoryCharacter)
}
@Dao interface PlaceDao {
 @Query("SELECT * FROM places WHERE projectId=:projectId ORDER BY name COLLATE NOCASE") fun observe(projectId:Long):Flow<List<StoryPlace>>
 @Insert suspend fun insert(item:StoryPlace):Long
 @Update suspend fun update(item:StoryPlace)
 @Delete suspend fun delete(item:StoryPlace)
}
@Dao interface WorldDao {
 @Query("SELECT * FROM world_entries WHERE projectId=:projectId ORDER BY kind,name COLLATE NOCASE") fun observe(projectId:Long):Flow<List<WorldEntry>>
 @Insert suspend fun insert(item:WorldEntry):Long
 @Update suspend fun update(item:WorldEntry)
 @Delete suspend fun delete(item:WorldEntry)
}
@Dao interface RelationshipDao {
 @Query("SELECT * FROM relationships WHERE projectId=:projectId ORDER BY fromCharacter,toCharacter") fun observe(projectId:Long):Flow<List<CharacterRelationship>>
 @Insert suspend fun insert(item:CharacterRelationship):Long
 @Update suspend fun update(item:CharacterRelationship)
 @Delete suspend fun delete(item:CharacterRelationship)
}
@Dao interface TimelineDao {
 @Query("SELECT * FROM timeline_events WHERE projectId=:projectId ORDER BY orderIndex,id") fun observe(projectId:Long):Flow<List<TimelineEvent>>
 @Insert suspend fun insert(item:TimelineEvent):Long
 @Update suspend fun update(item:TimelineEvent)
 @Delete suspend fun delete(item:TimelineEvent)
}
@Dao interface SnapshotDao {
 @Query("SELECT * FROM story_snapshots WHERE projectId=:projectId ORDER BY createdAt DESC") fun observe(projectId:Long):Flow<List<StorySnapshot>>
 @Insert suspend fun insert(item:StorySnapshot):Long
 @Delete suspend fun delete(item:StorySnapshot)
}
@Dao interface NoteDao {
 @Query("SELECT * FROM story_notes WHERE projectId=:projectId ORDER BY updatedAt DESC") fun observe(projectId:Long):Flow<List<StoryNote>>
 @Insert suspend fun insert(item:StoryNote):Long
 @Update suspend fun update(item:StoryNote)
 @Delete suspend fun delete(item:StoryNote)
}
@Dao interface GoalDao {
 @Query("SELECT * FROM writing_goals WHERE projectId=:projectId LIMIT 1") fun observe(projectId:Long):Flow<WritingGoal?>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(item:WritingGoal)
}
@Dao interface SettingsDao {
 @Query("SELECT * FROM app_settings WHERE `key`=:key LIMIT 1") fun observe(key:String):Flow<AppSetting?>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(setting:AppSetting)
}
@Dao interface NarrativeLinkDao {
 @Query("SELECT * FROM narrative_links WHERE projectId=:projectId ORDER BY chapterId,elementType,elementId") fun observe(projectId:Long):Flow<List<NarrativeLink>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insert(link:NarrativeLink):Long
 @Delete suspend fun delete(link:NarrativeLink)
}

@Database(entities=[StoryProject::class,StoryChapter::class,StoryScene::class,StoryCharacter::class,StoryPlace::class,WorldEntry::class,CharacterRelationship::class,TimelineEvent::class,StorySnapshot::class,StoryNote::class,WritingGoal::class,AppSetting::class,NarrativeLink::class],version=3,exportSchema=false)
abstract class StoryDatabase:RoomDatabase(){
 abstract fun projects():ProjectDao
 abstract fun chapters():ChapterDao
 abstract fun scenes():SceneDao
 abstract fun characters():CharacterDao
 abstract fun places():PlaceDao
 abstract fun world():WorldDao
 abstract fun relationships():RelationshipDao
 abstract fun timeline():TimelineDao
 abstract fun snapshots():SnapshotDao
 abstract fun notes():NoteDao
 abstract fun goals():GoalDao
 abstract fun settings():SettingsDao
 abstract fun narrativeLinks():NarrativeLinkDao
 companion object{
  @Volatile private var INSTANCE:StoryDatabase?=null
  private val MIGRATION_1_2=object:Migration(1,2){
   override fun migrate(db:SupportSQLiteDatabase){
    db.execSQL("CREATE TABLE IF NOT EXISTS scenes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,chapterId INTEGER NOT NULL,title TEXT NOT NULL,summary TEXT NOT NULL,goal TEXT NOT NULL,conflict TEXT NOT NULL,outcome TEXT NOT NULL,content TEXT NOT NULL,position INTEGER NOT NULL,updatedAt INTEGER NOT NULL,FOREIGN KEY(chapterId) REFERENCES chapters(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_scenes_chapterId ON scenes(chapterId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS characters (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,name TEXT NOT NULL,role TEXT NOT NULL,description TEXT NOT NULL,motivation TEXT NOT NULL,goal TEXT NOT NULL,need TEXT NOT NULL,flaw TEXT NOT NULL,arc TEXT NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_characters_projectId ON characters(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS places (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,name TEXT NOT NULL,description TEXT NOT NULL,rules TEXT NOT NULL,atmosphere TEXT NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_places_projectId ON places(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS world_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,kind TEXT NOT NULL,name TEXT NOT NULL,description TEXT NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_world_entries_projectId ON world_entries(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS relationships (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,fromCharacter TEXT NOT NULL,toCharacter TEXT NOT NULL,description TEXT NOT NULL,evolution TEXT NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_relationships_projectId ON relationships(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS timeline_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,title TEXT NOT NULL,storyDate TEXT NOT NULL,description TEXT NOT NULL,consequences TEXT NOT NULL,orderIndex INTEGER NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_timeline_events_projectId ON timeline_events(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS story_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,chapterId INTEGER,title TEXT NOT NULL,content TEXT NOT NULL,createdAt INTEGER NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_story_snapshots_projectId ON story_snapshots(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS story_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,title TEXT NOT NULL,content TEXT NOT NULL,category TEXT NOT NULL,updatedAt INTEGER NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_story_notes_projectId ON story_notes(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS writing_goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,targetWords INTEGER NOT NULL,currentWords INTEGER NOT NULL,targetDate TEXT NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_writing_goals_projectId ON writing_goals(projectId)")
    db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (`key` TEXT NOT NULL PRIMARY KEY,value TEXT NOT NULL)")
   }
  }
  private val MIGRATION_2_3=object:Migration(2,3){
   override fun migrate(db:SupportSQLiteDatabase){
    db.execSQL("CREATE TABLE IF NOT EXISTS narrative_links (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,chapterId INTEGER NOT NULL,elementType TEXT NOT NULL,elementId INTEGER NOT NULL,createdAt INTEGER NOT NULL,FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE,FOREIGN KEY(chapterId) REFERENCES chapters(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_narrative_links_projectId ON narrative_links(projectId)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_narrative_links_chapterId ON narrative_links(chapterId)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_narrative_links_chapterId_elementType_elementId ON narrative_links(chapterId,elementType,elementId)")
   }
  }
  fun get(context:Context):StoryDatabase=INSTANCE?: synchronized(this){INSTANCE?:Room.databaseBuilder(context.applicationContext,StoryDatabase::class.java,"escritor.db").addMigrations(MIGRATION_1_2,MIGRATION_2_3).build().also{INSTANCE=it}}
 }
}
