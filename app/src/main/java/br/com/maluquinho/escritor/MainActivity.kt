package br.com.maluquinho.escritor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = StoryDatabase.get(this)
        setContent { MaterialTheme { EscritorApp(database, this) } }
    }
}

private fun wordCount(text: String): Int =
    text.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+"))?.size ?: 0

private fun projectAsText(project: StoryProject, chapters: List<StoryChapter>): String = buildString {
    appendLine(project.title)
    appendLine("Formato: ${project.format}")
    if (project.synopsis.isNotBlank()) {
        appendLine()
        appendLine("SINOPSE")
        appendLine(project.synopsis)
    }
    chapters.forEachIndexed { index, chapter ->
        appendLine()
        appendLine("CAPÍTULO ${index + 1} — ${chapter.title}")
        appendLine()
        appendLine(chapter.content)
    }
}

private fun writeText(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
        it.write(content)
    } ?: error("Não foi possível abrir o arquivo de destino.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscritorApp(database: StoryDatabase, context: Context) {
    val projects by database.projects().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedProject by remember { mutableStateOf<StoryProject?>(null) }
    var selectedChapter by remember { mutableStateOf<StoryChapter?>(null) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterText by remember { mutableStateOf("") }
    var showNewProject by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newSynopsis by remember { mutableStateOf("") }
    var newFormat by remember { mutableStateOf("Romance") }
    var showTechniques by remember { mutableStateOf(false) }
    var deleteProject by remember { mutableStateOf<StoryProject?>(null) }
    var deleteChapter by remember { mutableStateOf<StoryChapter?>(null) }
    var saveLabel by remember { mutableStateOf("Salvo") }

    val chapters by selectedProject?.let { database.chapters().observeForProject(it.id) }
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList<StoryChapter>()) }

    var exportContent by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching { writeText(context, uri, exportContent) }
                .onSuccess { scope.launch { snackbar.showSnackbar("História exportada em TXT.") } }
                .onFailure { scope.launch { snackbar.showSnackbar("Falha ao exportar: ${it.message}") } }
        }
    }

    // Salvamento automático com debounce: evita gravar a cada tecla.
    LaunchedEffect(selectedChapter?.id, chapterTitle, chapterText) {
        val chapter = selectedChapter ?: return@LaunchedEffect
        saveLabel = "Salvando…"
        delay(650)
        database.chapters().update(
            chapter.copy(
                title = chapterTitle.ifBlank { "Sem título" },
                content = chapterText,
                updatedAt = System.currentTimeMillis()
            )
        )
        saveLabel = "Salvo automaticamente"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ESCRITOR", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(
                            when {
                                showTechniques -> "Biblioteca de escrita"
                                selectedChapter != null -> "Editor de histórias"
                                selectedProject != null -> selectedProject!!.title
                                else -> "Oficina de histórias"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    if (showTechniques || selectedChapter != null || selectedProject != null) {
                        IconButton(onClick = {
                            when {
                                showTechniques -> showTechniques = false
                                selectedChapter != null -> selectedChapter = null
                                else -> selectedProject = null
                            }
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                    } else {
                        Icon(Icons.Default.Book, contentDescription = "Escritor", modifier = Modifier.padding(start = 16.dp))
                    }
                },
                actions = {
                    when {
                        showTechniques -> Unit
                        selectedChapter != null -> IconButton(onClick = {
                            val chapter = selectedChapter!!
                            scope.launch {
                                database.chapters().update(
                                    chapter.copy(title = chapterTitle.ifBlank { "Sem título" }, content = chapterText, updatedAt = System.currentTimeMillis())
                                )
                                selectedChapter = null
                                snackbar.showSnackbar("Capítulo salvo.")
                            }
                        }) { Icon(Icons.Default.Save, contentDescription = "Salvar e voltar") }
                        selectedProject != null -> IconButton(onClick = {
                            exportContent = projectAsText(selectedProject!!, chapters)
                            exportLauncher.launch("${selectedProject!!.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").ifBlank { "historia" }}.txt")
                        }) { Icon(Icons.Default.Download, contentDescription = "Exportar história") }
                        else -> {
                            IconButton(onClick = { showTechniques = true }) {
                                Icon(Icons.Default.Lightbulb, contentDescription = "Técnicas de escrita")
                            }
                            IconButton(onClick = { showNewProject = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Novo projeto")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            showTechniques -> TechniquesScreen(Modifier.fillMaxSize().padding(padding))
            selectedChapter != null -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = chapterTitle,
                        onValueChange = { chapterTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título do capítulo") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${wordCount(chapterText)} palavras · ${chapterText.length} caracteres", style = MaterialTheme.typography.labelMedium)
                        Text(saveLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (chapterText.isEmpty()) {
                            Text("Comece a escrever sua história…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = chapterText,
                            onValueChange = { chapterText = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 28.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        "O texto é salvo automaticamente neste dispositivo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            selectedProject != null -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
                    Text(selectedProject!!.format.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (selectedProject!!.synopsis.isNotBlank()) {
                        Text(selectedProject!!.synopsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Capítulos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        FilledTonalButton(onClick = {
                            scope.launch {
                                val title = "Capítulo ${chapters.size + 1}"
                                val id = database.chapters().insert(
                                    StoryChapter(projectId = selectedProject!!.id, title = title, position = chapters.size)
                                )
                                selectedChapter = StoryChapter(id = id, projectId = selectedProject!!.id, title = title, position = chapters.size)
                                chapterTitle = title
                                chapterText = ""
                            }
                        }) { Icon(Icons.Default.Add, contentDescription = null); Text(" Novo") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (chapters.isEmpty()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Sua história começa aqui.", fontWeight = FontWeight.SemiBold)
                                Text("Crie o primeiro capítulo. O conteúdo será salvo automaticamente.")
                            }
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chapters, key = { it.id }) { chapter ->
                            Card {
                                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                                    Column(
                                        Modifier.weight(1f).padding(vertical = 8.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                selectedChapter = chapter
                                                chapterTitle = chapter.title
                                                chapterText = chapter.content
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Column(Modifier.fillMaxWidth()) {
                                                Text(chapter.title, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "${wordCount(chapter.content)} palavras",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { deleteChapter = chapter }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir capítulo")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Suas histórias", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Um espaço para imaginar, estruturar e escrever.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (projects.isEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("Toda história começa com uma ideia.", style = MaterialTheme.typography.titleMedium)
                                    Text("Crie um projeto para reunir capítulos, personagens e universos.")
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { showNewProject = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Text(" Criar projeto")
                                    }
                                }
                            }
                        }
                    }
                    items(projects, key = { it.id }) { project ->
                        Card(onClick = { selectedProject = project }) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text(project.format, color = MaterialTheme.colorScheme.primary)
                                    if (project.synopsis.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(project.synopsis, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { deleteProject = project }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir projeto")
                                }
                            }
                        }
                    }
                    item {
                        Text("Seus manuscritos permanecem no dispositivo.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showNewProject) {
        AlertDialog(
            onDismissRequest = { showNewProject = false },
            title = { Text("Novo projeto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Título da história") }, singleLine = true)
                    OutlinedTextField(value = newSynopsis, onValueChange = { newSynopsis = it }, label = { Text("Sinopse (opcional)") }, minLines = 2)
                    Text("Formato", style = MaterialTheme.typography.labelLarge)
                    listOf("Romance", "Conto", "Light novel", "Web novel", "Roteiro", "Poesia", "Crônica", "Fanfic").chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { format ->
                                FilterChip(
                                    selected = newFormat == format,
                                    onClick = { newFormat = format },
                                    label = { Text(format) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) scope.launch {
                        database.projects().insert(
                            StoryProject(title = newTitle.trim(), format = newFormat, synopsis = newSynopsis.trim())
                        )
                        newTitle = ""
                        newSynopsis = ""
                        newFormat = "Romance"
                        showNewProject = false
                    }
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { showNewProject = false }) { Text("Cancelar") } }
        )
    }

    deleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteProject = null },
            title = { Text("Excluir projeto?") },
            text = { Text("“${project.title}” e todos os seus capítulos serão apagados deste dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { database.projects().delete(project) }
                    deleteProject = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteProject = null }) { Text("Cancelar") } }
        )
    }

    deleteChapter?.let { chapter ->
        AlertDialog(
            onDismissRequest = { deleteChapter = null },
            title = { Text("Excluir capítulo?") },
            text = { Text("“${chapter.title}” será apagado permanentemente.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { database.chapters().delete(chapter) }
                    deleteChapter = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteChapter = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TechniquesScreen(modifier: Modifier = Modifier) {
    val sections = listOf(
        "Construção de enredo" to "Três atos, cinco atos, Snowflake Method, escaleta, beat sheet, causa e consequência, subtramas e reviravoltas.",
        "Personagens" to "Desejo e necessidade, contradições, arco positivo ou negativo, motivação, conflito interno e voz individual.",
        "Narrador e perspectiva" to "Primeira e terceira pessoa, focalização, narrador não confiável, distância narrativa e discurso indireto livre.",
        "Cenas e ritmo" to "Objetivo, obstáculo e resultado; entrada tardia, saída antecipada, escalada, pausa, gancho e cliffhanger.",
        "Diálogos" to "Subtexto, intenção oculta, silêncio, evasão, vozes distintas e exposição dramatizada.",
        "Descrição e estilo" to "Detalhes sensoriais, imagens concretas, metáforas, símbolos, motivos recorrentes e show, don't tell.",
        "Contos" to "Unidade de efeito, economia narrativa, elipse, sugestão e finais de ressonância.",
        "Romances" to "Arcos longos, subtramas, distribuição de revelações, continuidade e transformação de relações.",
        "Light novels" to "Capítulos ágeis, elenco recorrente, diálogos, arcos por volume e ilustrações complementares.",
        "Web novels" to "Ritmo episódico, ganchos, payoff, recapitulação contextual e continuidade serial.",
        "Poesia" to "Métrica, ritmo, rima, aliteração, assonância, enjambement, metáfora e campo semântico.",
        "Revisão" to "Revisão estrutural, de cenas, continuidade, estilo, linha e prova final."
    )
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Ferramentas de criação", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Técnicas são recursos, não regras obrigatórias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(sections) { (title, description) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
