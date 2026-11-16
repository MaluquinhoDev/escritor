package br.com.maluquinho.escritor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.maluquinho.escritor.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = StoryDatabase.get(this)
        setContent { MaterialTheme { EscritorApp(database) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscritorApp(database: StoryDatabase) {
    val projects by database.projects().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var selectedProject by remember { mutableStateOf<StoryProject?>(null) }
    var selectedChapter by remember { mutableStateOf<StoryChapter?>(null) }
    var chapterTitle by remember { mutableStateOf("Capítulo 1") }
    var chapterText by remember { mutableStateOf("") }
    var showNewProject by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newFormat by remember { mutableStateOf("Romance") }
    var notice by remember { mutableStateOf("") }

    val chapters by selectedProject?.let { database.chapters().observeForProject(it.id) }
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList<StoryChapter>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ESCRITOR", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Oficina de histórias", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    if (selectedProject != null) {
                        IconButton(onClick = { selectedProject = null; selectedChapter = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    } else {
                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
                    }
                },
                actions = {
                    if (selectedProject == null) {
                        IconButton(onClick = { showNewProject = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Novo projeto")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedProject == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Suas histórias", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Projetos, mundos e narrativas em um só lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
                if (projects.isEmpty()) {
                    item {
                        Card {
                            Column(Modifier.padding(20.dp)) {
                                Text("Uma história começa com uma ideia.", style = MaterialTheme.typography.titleMedium)
                                Text("Crie seu primeiro projeto para organizar capítulos, personagens e universo.")
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { showNewProject = true }) { Icon(Icons.Default.Add, null); Text(" Criar projeto") }
                            }
                        }
                    }
                }
                items(projects, key = { it.id }) { project ->
                    Card(onClick = { selectedProject = project; selectedChapter = null }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(project.format, color = MaterialTheme.colorScheme.primary)
                                if (project.synopsis.isNotBlank()) Text(project.synopsis, maxLines = 2)
                            }
                            IconButton(onClick = { scope.launch { database.projects().delete(project) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir projeto")
                            }
                        }
                    }
                }
                item { Text("Escrita local • seus textos ficam no dispositivo", style = MaterialTheme.typography.labelSmall) }
            }
        } else if (selectedChapter == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
                Text(selectedProject!!.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(selectedProject!!.format, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estrutura", style = MaterialTheme.typography.titleLarge)
                    FilledTonalButton(onClick = {
                        scope.launch {
                            val id = database.chapters().insert(
                                StoryChapter(projectId = selectedProject!!.id, title = "Capítulo ${chapters.size + 1}", position = chapters.size)
                            )
                            selectedChapter = StoryChapter(id = id, projectId = selectedProject!!.id, title = "Capítulo ${chapters.size + 1}", position = chapters.size)
                            chapterTitle = selectedChapter!!.title
                            chapterText = ""
                        }
                    }) { Icon(Icons.Default.Add, null); Text(" Capítulo") }
                }
                Spacer(Modifier.height(8.dp))
                if (chapters.isEmpty()) {
                    Text("Nenhum capítulo ainda. A estrutura da história começa aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chapters, key = { it.id }) { chapter ->
                        Card(onClick = {
                            selectedChapter = chapter
                            chapterTitle = chapter.title
                            chapterText = chapter.content
                        }) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(chapter.title, fontWeight = FontWeight.SemiBold)
                                Text(if (chapter.content.isBlank()) "Texto vazio" else "${chapter.content.trim().split(Regex("\\s+")).size} palavras", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Título do capítulo") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${chapterText.trim().let { if (it.isBlank()) 0 else it.split(Regex("\\s+")).size }} palavras", style = MaterialTheme.typography.labelMedium)
                    Text("Rascunho local", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (chapterText.isEmpty()) {
                        Text("Comece a escrever sua história…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = chapterText,
                        onValueChange = { chapterText = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 27.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            selectedChapter?.let {
                                database.chapters().update(it.copy(title = chapterTitle, content = chapterText, updatedAt = System.currentTimeMillis()))
                                notice = "Capítulo salvo"
                            }
                            selectedChapter = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Default.Save, null); Text(" Salvar capítulo") }
            }
        }
    }

    if (showNewProject) {
        AlertDialog(
            onDismissRequest = { showNewProject = false },
            title = { Text("Novo projeto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Nome da história") }, singleLine = true)
                    Text("Formato", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Romance", "Conto", "Light novel", "Web novel").forEach { format ->
                            FilterChip(selected = newFormat == format, onClick = { newFormat = format }, label = { Text(format) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) scope.launch {
                        database.projects().insert(StoryProject(title = newTitle.trim(), format = newFormat))
                        newTitle = ""; newFormat = "Romance"; showNewProject = false
                    }
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { showNewProject = false }) { Text("Cancelar") } }
        )
    }
}
