package br.com.maluquinho.escritor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.maluquinho.escritor.data.*
import kotlinx.coroutines.launch

private val tabs = listOf("Cenas","Personagens","Lugares","Universo","Relações","Cronologia","Notas","Meta","Revisão","Versões")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPlanningScreen(database:StoryDatabase, project:StoryProject, modifier:Modifier=Modifier) {
 var tab by remember { mutableIntStateOf(0) }
 var name by remember { mutableStateOf("") }
 var body by remember { mutableStateOf("") }
 var extra by remember { mutableStateOf("") }
 var kind by remember { mutableStateOf("Objeto") }
 var chapterId by remember { mutableStateOf<Long?>(null) }
 val scope= rememberCoroutineScope()
 val chapters by database.chapters().observeForProject(project.id).collectAsStateWithLifecycle(emptyList())
 val characters by database.characters().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val places by database.places().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val world by database.world().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val relations by database.relationships().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val events by database.timeline().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val notes by database.notes().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val goal by database.goals().observe(project.id).collectAsStateWithLifecycle(null)
 val snapshots by database.snapshots().observe(project.id).collectAsStateWithLifecycle(emptyList())
 val scenes by chapterId?.let { database.scenes().observeForChapter(it) }?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList<StoryScene>()) }
 fun clear(){name="";body="";extra=""}
 Column(modifier.fillMaxSize()) {
  ScrollableTabRow(selectedTabIndex=tab) { tabs.forEachIndexed { i,t -> Tab(selected=tab==i,onClick={tab=i;clear()},text={Text(t)}) } }
  Column(Modifier.fillMaxSize().padding(14.dp)) {
   when(tab) {
    0 -> {
     Text("Cenas do capítulo",style=MaterialTheme.typography.titleLarge)
     var expanded by remember { mutableStateOf(false) }
     ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}) {
      OutlinedTextField(chapters.firstOrNull{it.id==chapterId}?.title ?: "Selecione um capítulo",{},Modifier.fillMaxWidth().menuAnchor(),readOnly=true,label={Text("Capítulo")})
      ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { chapters.forEach { c -> DropdownMenuItem(text={Text(c.title)},onClick={chapterId=c.id;expanded=false}) } }
     }
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nome da cena")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Objetivo e resumo")},minLines=2)
     OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Conflito / resultado")},minLines=2)
     Button(onClick={val id=chapterId;if(id!=null&&name.isNotBlank())scope.launch{database.scenes().insert(StoryScene(chapterId=id,title=name.trim(),summary=body,conflict=extra));clear()}},enabled=chapterId!=null&&name.isNotBlank()){Icon(Icons.Default.Add,null);Text(" Adicionar cena")}
     LazyColumn { items(scenes,key={it.id}) { s -> PlanningItem(s.title,s.summary+" "+s.conflict){scope.launch{database.scenes().delete(s)}} } }
    }
    1 -> {
     Text("Personagens",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nome")})
     OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Papel")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Descrição, motivação, desejo, necessidade e arco")},minLines=3)
     Button(onClick={if(name.isNotBlank())scope.launch{database.characters().insert(StoryCharacter(projectId=project.id,name=name.trim(),role=extra,description=body,motivation=body));clear()}},enabled=name.isNotBlank()){Text("Adicionar personagem")}
     LazyColumn{items(characters,key={it.id}){c->PlanningItem(c.name,c.role+" "+c.description){scope.launch{database.characters().delete(c)}}}}
    }
    2 -> {
     Text("Lugares",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nome do lugar")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Descrição e atmosfera")},minLines=3)
     OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Regras do lugar")})
     Button(onClick={if(name.isNotBlank())scope.launch{database.places().insert(StoryPlace(projectId=project.id,name=name.trim(),description=body,atmosphere=body,rules=extra));clear()}},enabled=name.isNotBlank()){Text("Adicionar lugar")}
     LazyColumn{items(places,key={it.id}){p->PlanningItem(p.name,p.description+" "+p.rules){scope.launch{database.places().delete(p)}}}}
    }
    3 -> {
     Text("Elementos do universo",style=MaterialTheme.typography.titleLarge)
     Row { listOf("Objeto","Organização","Regra","Espécie","Tecnologia").forEach{FilterChip(selected=kind==it,onClick={kind=it},label={Text(it)})} }
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nome")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Descrição e funcionamento")},minLines=3)
     Button(onClick={if(name.isNotBlank())scope.launch{database.world().insert(WorldEntry(projectId=project.id,kind=kind,name=name.trim(),description=body));clear()}},enabled=name.isNotBlank()){Text("Adicionar elemento")}
     LazyColumn{items(world,key={it.id}){w->PlanningItem(w.kind+": "+w.name,w.description){scope.launch{database.world().delete(w)}}}}
    }
    4 -> {
     Text("Relações",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Personagem A")})
     OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Personagem B")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Relação e evolução")},minLines=2)
     Button(onClick={if(name.isNotBlank()&&extra.isNotBlank())scope.launch{database.relationships().insert(CharacterRelationship(projectId=project.id,fromCharacter=name.trim(),toCharacter=extra.trim(),description=body,evolution=body));clear()}},enabled=name.isNotBlank()&&extra.isNotBlank()){Text("Registrar relação")}
     LazyColumn{items(relations,key={it.id}){r->PlanningItem(r.fromCharacter+" ↔ "+r.toCharacter,r.description){scope.launch{database.relationships().delete(r)}}}}
    }
    5 -> {
     Text("Cronologia",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Acontecimento")})
     OutlinedTextField(extra,{extra=it},Modifier.fillMaxWidth(),label={Text("Data / período")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Descrição e consequências")},minLines=3)
     Button(onClick={if(name.isNotBlank())scope.launch{database.timeline().insert(TimelineEvent(projectId=project.id,title=name.trim(),storyDate=extra,description=body,consequences=body,orderIndex=events.size));clear()}},enabled=name.isNotBlank()){Text("Adicionar acontecimento")}
     LazyColumn{items(events,key={it.id}){e->PlanningItem(e.storyDate+" — "+e.title,e.description){scope.launch{database.timeline().delete(e)}}}}
    }
    6 -> {
     Text("Notas de pesquisa",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Título")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Conteúdo")},minLines=4)
     Button(onClick={if(name.isNotBlank())scope.launch{database.notes().insert(StoryNote(projectId=project.id,title=name.trim(),content=body));clear()}},enabled=name.isNotBlank()){Text("Salvar nota")}
     LazyColumn{items(notes,key={it.id}){n->PlanningItem(n.title,n.content){scope.launch{database.notes().delete(n)}}}}
    }
    7 -> {
     Text("Meta de escrita",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Meta de palavras")})
     OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("Data-alvo (opcional)")})
     Button(onClick={val target=name.toIntOrNull();if(target!=null&&target>0)scope.launch{database.goals().save(WritingGoal(projectId=project.id,targetWords=target,targetDate=body));clear()}},enabled=(name.toIntOrNull()?:0)>0){Text("Salvar meta")}
     goal?.let{Text("Meta definida: "+it.targetWords+" palavras")}
    }
    8 -> {
     val totalWords = chapters.sumOf { it.content.trim().split(Regex("\\s+")).count { word -> word.isNotBlank() } }
     Text("Diagnóstico textual",style=MaterialTheme.typography.titleLarge)
     Text("Capítulos: "+chapters.size+" · Palavras: "+totalWords)
     val allText = chapters.joinToString(" ") { it.content }
     val words = Regex("[\\p{L}\\p{N}']+").findAll(allText.lowercase()).map { it.value }.toList()
     val repeated = words.groupingBy { it }.eachCount().filter { it.value >= 5 }.entries.sortedByDescending { it.value }.take(12)
     val longSentences = allText.split(Regex("[.!?]+")).count { it.trim().split(Regex("\\s+")).size > 30 }
     Text("Frases com mais de 30 palavras: "+longSentences)
     Text("Termos recorrentes (5+ ocorrências)",style=MaterialTheme.typography.titleMedium)
     repeated.forEach { Text(it.key+" — "+it.value+" vezes") }
     Text("Relatório heurístico: repetições e extensão são alertas para revisão, não erros automáticos.",style=MaterialTheme.typography.bodySmall)
    }
    9 -> {
     Text("Versões e recuperação",style=MaterialTheme.typography.titleLarge)
     Text("Crie um ponto de restauração com o conteúdo atual dos capítulos.")
     Button(onClick={scope.launch{chapters.forEach { chapter -> database.snapshots().insert(StorySnapshot(projectId=project.id,chapterId=chapter.id,title=chapter.title,content=chapter.content)) }}}){Text("Criar snapshot dos capítulos")}
     LazyColumn { items(snapshots,key={it.id}) { snap -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(snap.title,style=MaterialTheme.typography.titleMedium);Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(snap.createdAt)),style=MaterialTheme.typography.bodySmall);Text(snap.content.take(180),style=MaterialTheme.typography.bodySmall);Row {
 TextButton(onClick={scope.launch{val id=snap.chapterId;val original=chapters.firstOrNull{it.id==id};if(original!=null)database.chapters().update(original.copy(content=snap.content,updatedAt=System.currentTimeMillis()))}} ,enabled=snap.chapterId!=null){Text("Restaurar")}
 TextButton(onClick={scope.launch{database.snapshots().delete(snap)}}){Text("Excluir")}
}}} } }
    }
   }
  }
 }
}
@Composable private fun PlanningItem(title:String,description:String,onDelete:()->Unit){
 Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodySmall)};IconButton(onClick=onDelete){Icon(Icons.Default.Delete,"Excluir")}}}
}
