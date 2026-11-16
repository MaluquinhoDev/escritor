package br.com.maluquinho.escritor

import android.content.Context
import android.net.Uri
import br.com.maluquinho.escritor.data.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.putText(key: String, value: String) { put(key, value) }
private fun JSONObject.text(key: String, fallback: String = ""): String = optString(key, fallback).takeUnless { it == "null" } ?: fallback

suspend fun exportProjectArchive(database: StoryDatabase, project: StoryProject): String {
    val chapters = database.chapters().observeForProject(project.id).first()
    val root = JSONObject()
        .put("format", "escritor-project")
        .put("schemaVersion", 1)
        .put("project", JSONObject().put("title", project.title).put("format", project.format).put("synopsis", project.synopsis).put("createdAt", project.createdAt))
    val chapterArray = JSONArray()
    val sceneArray = JSONArray()
    chapters.forEach { chapter ->
        chapterArray.put(JSONObject().put("id", chapter.id).put("title", chapter.title).put("content", chapter.content).put("position", chapter.position).put("updatedAt", chapter.updatedAt))
        database.scenes().observeForChapter(chapter.id).first().forEach { scene ->
            sceneArray.put(JSONObject().put("id", scene.id).put("chapterId", chapter.id).put("title", scene.title).put("summary", scene.summary).put("goal", scene.goal).put("conflict", scene.conflict).put("outcome", scene.outcome).put("content", scene.content).put("position", scene.position).put("updatedAt", scene.updatedAt))
        }
    }
    root.put("chapters", chapterArray).put("scenes", sceneArray)
    root.put("characters", JSONArray().also { a -> database.characters().observe(project.id).first().forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("role",it.role).put("description",it.description).put("motivation",it.motivation).put("goal",it.goal).put("need",it.need).put("flaw",it.flaw).put("arc",it.arc)) } })
    root.put("places", JSONArray().also { a -> database.places().observe(project.id).first().forEach { a.put(JSONObject().put("id",it.id).put("name",it.name).put("description",it.description).put("rules",it.rules).put("atmosphere",it.atmosphere)) } })
    root.put("world", JSONArray().also { a -> database.world().observe(project.id).first().forEach { a.put(JSONObject().put("id",it.id).put("kind",it.kind).put("name",it.name).put("description",it.description)) } })
    root.put("relationships", JSONArray().also { a -> database.relationships().observe(project.id).first().forEach { a.put(JSONObject().put("fromCharacter",it.fromCharacter).put("toCharacter",it.toCharacter).put("description",it.description).put("evolution",it.evolution)) } })
    root.put("events", JSONArray().also { a -> database.timeline().observe(project.id).first().forEach { a.put(JSONObject().put("title",it.title).put("storyDate",it.storyDate).put("description",it.description).put("consequences",it.consequences).put("orderIndex",it.orderIndex)) } })
    root.put("notes", JSONArray().also { a -> database.notes().observe(project.id).first().forEach { a.put(JSONObject().put("title",it.title).put("content",it.content).put("category",it.category).put("updatedAt",it.updatedAt)) } })
    root.put("snapshots", JSONArray().also { a -> database.snapshots().observe(project.id).first().forEach { a.put(JSONObject().put("chapterId",it.chapterId ?: JSONObject.NULL).put("title",it.title).put("content",it.content).put("createdAt",it.createdAt)) } })
    root.put("goal", database.goals().observe(project.id).first()?.let { JSONObject().put("targetWords",it.targetWords).put("currentWords",it.currentWords).put("targetDate",it.targetDate) } ?: JSONObject.NULL)
    root.put("links", JSONArray().also { a -> database.narrativeLinks().observe(project.id).first().forEach { a.put(JSONObject().put("chapterId",it.chapterId).put("elementType",it.elementType).put("elementId",it.elementId).put("createdAt",it.createdAt)) } })
    return root.toString(2)
}

suspend fun exportPlanningArchive(database: StoryDatabase, project: StoryProject): String {
    val full = JSONObject(exportProjectArchive(database, project))
    full.put("format", "escritor-planning")
    full.remove("project")
    full.remove("snapshots")
    full.remove("goal")
    full.remove("chapters")
    full.remove("links")
    return full.toString(2)
}

suspend fun importProjectArchive(database: StoryDatabase, jsonText: String): Long {
    val root = JSONObject(jsonText)
    require(root.optString("format") == "escritor-project" && root.optInt("schemaVersion") == 1) { "Arquivo de projeto Escritor inválido ou versão não suportada." }
    val p = root.getJSONObject("project")
    val projectId = database.projects().insert(StoryProject(title=p.text("title","Projeto importado"),format=p.text("format","Romance"),synopsis=p.text("synopsis")))
    val chapterIds = mutableMapOf<Long,Long>()
    val chapterArray = root.optJSONArray("chapters") ?: JSONArray()
    for (i in 0 until chapterArray.length()) {
        val item=chapterArray.getJSONObject(i)
        val newId=database.chapters().insert(StoryChapter(projectId=projectId,title=item.text("title","Capítulo"),content=item.text("content"),position=item.optInt("position",i),updatedAt=item.optLong("updatedAt",System.currentTimeMillis())))
        chapterIds[item.optLong("id")] = newId
    }
    importPlanningRows(database,projectId,root,chapterIds)
    val oldToNew = mutableMapOf<String,MutableMap<Long,Long>>()
    suspend fun <T> mapRows(key:String, insert:suspend (JSONObject)->Long) {
        val arr=root.optJSONArray(key) ?: return
        val map=mutableMapOf<Long,Long>()
        for(i in 0 until arr.length()) { val obj=arr.getJSONObject(i); val old=obj.optLong("id",-(i+1).toLong()); map[old]=insert(obj) }
        oldToNew[key]=map
    }
    mapRows("characters") { o -> database.characters().insert(StoryCharacter(projectId=projectId,name=o.text("name","Personagem"),role=o.text("role"),description=o.text("description"),motivation=o.text("motivation"),goal=o.text("goal"),need=o.text("need"),flaw=o.text("flaw"),arc=o.text("arc"))) }
    mapRows("places") { o -> database.places().insert(StoryPlace(projectId=projectId,name=o.text("name","Lugar"),description=o.text("description"),rules=o.text("rules"),atmosphere=o.text("atmosphere"))) }
    mapRows("world") { o -> database.world().insert(WorldEntry(projectId=projectId,kind=o.text("kind","Objeto"),name=o.text("name","Elemento"),description=o.text("description"))) }
    importOtherPlanning(database,projectId,root,oldToNew)
    val snapshots=root.optJSONArray("snapshots") ?: JSONArray()
    for(i in 0 until snapshots.length()) { val o=snapshots.getJSONObject(i); val old=o.optLong("chapterId",0L); database.snapshots().insert(StorySnapshot(projectId=projectId,chapterId=chapterIds[old],title=o.text("title","Snapshot"),content=o.text("content"),createdAt=o.optLong("createdAt",System.currentTimeMillis()))) }
    if(!root.isNull("goal")) { val o=root.getJSONObject("goal"); database.goals().save(WritingGoal(projectId=projectId,targetWords=o.optInt("targetWords",500),currentWords=o.optInt("currentWords",0),targetDate=o.text("targetDate"))) }
    importLinks(database,projectId,root,chapterIds,oldToNew)
    return projectId
}

suspend fun importPlanningArchive(database: StoryDatabase, projectId: Long, jsonText: String) {
    val root=JSONObject(jsonText)
    require(root.optString("format") == "escritor-planning" && root.optInt("schemaVersion") == 1) { "Arquivo de planejamento inválido ou versão não suportada." }
    val existing=database.chapters().observeForProject(projectId).first()
    val chapterIds=mutableMapOf<Long,Long>()
    val oldChapterArray=root.optJSONArray("chapters") ?: JSONArray()
    for(i in 0 until oldChapterArray.length()) {
        val o=oldChapterArray.getJSONObject(i)
        val title=o.text("title","Capítulo")
        val match=existing.firstOrNull { it.title.equals(title,true) }
        chapterIds[o.optLong("id",-(i+1).toLong())]=match?.id ?: database.chapters().insert(StoryChapter(projectId=projectId,title=title,position=existing.size+i))
    }
    importPlanningRows(database,projectId,root,chapterIds)
    val maps=mutableMapOf<String,MutableMap<Long,Long>>()
    suspend fun add(key:String, insert:suspend (JSONObject)->Long) {
        val arr=root.optJSONArray(key) ?: return
        val map=mutableMapOf<Long,Long>()
        for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); map[o.optLong("id",-(i+1).toLong())]=insert(o) }
        maps[key]=map
    }
    add("characters") { o -> database.characters().insert(StoryCharacter(projectId=projectId,name=o.text("name","Personagem"),role=o.text("role"),description=o.text("description"),motivation=o.text("motivation"),goal=o.text("goal"),need=o.text("need"),flaw=o.text("flaw"),arc=o.text("arc"))) }
    add("places") { o -> database.places().insert(StoryPlace(projectId=projectId,name=o.text("name","Lugar"),description=o.text("description"),rules=o.text("rules"),atmosphere=o.text("atmosphere"))) }
    add("world") { o -> database.world().insert(WorldEntry(projectId=projectId,kind=o.text("kind","Objeto"),name=o.text("name","Elemento"),description=o.text("description"))) }
    importOtherPlanning(database,projectId,root,maps)
    importLinks(database,projectId,root,chapterIds,maps)
}

private suspend fun importPlanningRows(database: StoryDatabase, projectId: Long, root: JSONObject, chapterIds: Map<Long,Long>) {
    val scenes=root.optJSONArray("scenes") ?: JSONArray()
    for(i in 0 until scenes.length()) { val o=scenes.getJSONObject(i); val chapter=chapterIds[o.optLong("chapterId")] ?: continue
        database.scenes().insert(StoryScene(chapterId=chapter,title=o.text("title","Cena"),summary=o.text("summary"),goal=o.text("goal"),conflict=o.text("conflict"),outcome=o.text("outcome"),content=o.text("content"),position=o.optInt("position",i),updatedAt=o.optLong("updatedAt",System.currentTimeMillis())))
    }
}

private suspend fun importOtherPlanning(database: StoryDatabase, projectId: Long, root: JSONObject, maps: MutableMap<String,MutableMap<Long,Long>>) {
    val rel=root.optJSONArray("relationships") ?: JSONArray()
    for(i in 0 until rel.length()) { val o=rel.getJSONObject(i); database.relationships().insert(CharacterRelationship(projectId=projectId,fromCharacter=o.text("fromCharacter"),toCharacter=o.text("toCharacter"),description=o.text("description"),evolution=o.text("evolution"))) }
    val events=root.optJSONArray("events") ?: JSONArray()
    for(i in 0 until events.length()) { val o=events.getJSONObject(i); database.timeline().insert(TimelineEvent(projectId=projectId,title=o.text("title","Acontecimento"),storyDate=o.text("storyDate"),description=o.text("description"),consequences=o.text("consequences"),orderIndex=o.optInt("orderIndex",i))) }
    val notes=root.optJSONArray("notes") ?: JSONArray()
    for(i in 0 until notes.length()) { val o=notes.getJSONObject(i); database.notes().insert(StoryNote(projectId=projectId,title=o.text("title","Nota"),content=o.text("content"),category=o.text("category","Pesquisa"),updatedAt=o.optLong("updatedAt",System.currentTimeMillis()))) }
}

private suspend fun importLinks(database: StoryDatabase, projectId: Long, root: JSONObject, chapterIds: Map<Long,Long>, maps: Map<String,Map<Long,Long>>) {
    val arr=root.optJSONArray("links") ?: return
    val keyByType=mapOf("PERSONAGEM" to "characters","LUGAR" to "places","UNIVERSO" to "world")
    for(i in 0 until arr.length()) {
        val o=arr.getJSONObject(i); val chapter=chapterIds[o.optLong("chapterId")] ?: continue
        val type=o.text("elementType"); val source=keyByType[type] ?: continue
        val element=maps[source]?.get(o.optLong("elementId")) ?: continue
        database.narrativeLinks().insert(NarrativeLink(projectId=projectId,chapterId=chapter,elementType=type,elementId=element,createdAt=o.optLong("createdAt",System.currentTimeMillis())))
    }
}

fun readArchiveText(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("Não foi possível abrir o arquivo selecionado.")

suspend fun importChapterText(database: StoryDatabase, projectId: Long, fileName: String, text: String): Long {
    require(text.isNotBlank()) { "O arquivo está vazio." }
    val title=fileName.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Capítulo importado" }
    val chapters=database.chapters().observeForProject(projectId).first()
    return database.chapters().insert(StoryChapter(projectId=projectId,title=title,content=text,position=chapters.size))
}
