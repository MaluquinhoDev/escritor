package br.com.maluquinho.escritor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.maluquinho.escritor.data.*
import kotlinx.coroutines.launch

private data class NarrativeOption(val id: Long, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarrativeConnectionsScreen(database: StoryDatabase, project: StoryProject, modifier: Modifier = Modifier) {
    val chapters by database.chapters().observeForProject(project.id).collectAsStateWithLifecycle(emptyList())
    val characters by database.characters().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val places by database.places().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val world by database.world().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val events by database.timeline().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val notes by database.notes().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val links by database.narrativeLinks().observe(project.id).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var selectedChapterId by remember(chapters) { mutableStateOf(chapters.firstOrNull()?.id) }
    var selectedType by remember { mutableStateOf("PERSONAGEM") }
    var selectedElementId by remember { mutableStateOf<Long?>(null) }
    var chapterMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var elementMenu by remember { mutableStateOf(false) }

    val options = when (selectedType) {
        "PERSONAGEM" -> characters.map { NarrativeOption(it.id, it.name) }
        "LUGAR" -> places.map { NarrativeOption(it.id, it.name) }
        "UNIVERSO" -> world.map { NarrativeOption(it.id, "${it.kind}: ${it.name}") }
        "EVENTO" -> events.map { NarrativeOption(it.id, it.title) }
        else -> notes.map { NarrativeOption(it.id, it.title) }
    }
    LaunchedEffect(selectedType, options) {
        if (options.none { it.id == selectedElementId }) selectedElementId = options.firstOrNull()?.id
    }
    fun elementName(type: String, id: Long): String = when (type) {
        "PERSONAGEM" -> characters.firstOrNull { it.id == id }?.name
        "LUGAR" -> places.firstOrNull { it.id == id }?.name
        "UNIVERSO" -> world.firstOrNull { it.id == id }?.let { "${it.kind}: ${it.name}" }
        "EVENTO" -> events.firstOrNull { it.id == id }?.title
        "NOTA" -> notes.firstOrNull { it.id == id }?.title
        else -> null
    } ?: "Registro removido"
    fun typeLabel(type: String) = when (type) {
        "PERSONAGEM" -> "Personagem"
        "LUGAR" -> "Lugar"
        "UNIVERSO" -> "Elemento do universo"
        "EVENTO" -> "Acontecimento"
        else -> "Nota"
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Conexões entre planejamento e manuscrito", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Associe registros narrativos aos capítulos em que aparecem ou produzem consequências. Os vínculos usam IDs internos, não nomes digitados, para continuarem ligados após renomeações.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Novo vínculo", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(expanded = chapterMenu, onExpandedChange = { chapterMenu = it }) {
                    OutlinedTextField(value = chapters.firstOrNull { it.id == selectedChapterId }?.title ?: "Nenhum capítulo", onValueChange = {}, readOnly = true, label = { Text("Capítulo") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                        chapters.forEach { chapter -> DropdownMenuItem(text = { Text(chapter.title) }, onClick = { selectedChapterId = chapter.id; chapterMenu = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
                    OutlinedTextField(value = typeLabel(selectedType), onValueChange = {}, readOnly = true, label = { Text("Tipo de registro") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf("PERSONAGEM", "LUGAR", "UNIVERSO", "EVENTO", "NOTA").forEach { type ->
                            DropdownMenuItem(text = { Text(typeLabel(type)) }, onClick = { selectedType = type; typeMenu = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = elementMenu, onExpandedChange = { elementMenu = it }) {
                    OutlinedTextField(value = options.firstOrNull { it.id == selectedElementId }?.label ?: "Nenhum registro disponível", onValueChange = {}, readOnly = true, label = { Text("Registro do planejamento") }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = elementMenu, onDismissRequest = { elementMenu = false }) {
                        options.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { selectedElementId = option.id; elementMenu = false }) }
                    }
                }
                Button(onClick = {
                    val chapterId = selectedChapterId
                    val elementId = selectedElementId
                    if (chapterId != null && elementId != null) scope.launch {
                        database.narrativeLinks().insert(NarrativeLink(projectId = project.id, chapterId = chapterId, elementType = selectedType, elementId = elementId))
                    }
                }, enabled = selectedChapterId != null && selectedElementId != null && options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("Vincular ao capítulo")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Mapa de presença narrativa", style = MaterialTheme.typography.titleMedium)
        if (links.isEmpty()) {
            Text("Nenhum vínculo cadastrado.", modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(links, key = { it.id }) { link ->
                    val chapterTitle = chapters.firstOrNull { it.id == link.chapterId }?.title ?: "Capítulo removido"
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(elementName(link.elementType, link.elementId), style = MaterialTheme.typography.titleSmall)
                                Text("${typeLabel(link.elementType)} · $chapterTitle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { scope.launch { database.narrativeLinks().delete(link) } }) { Text("Remover") }
                        }
                    }
                }
            }
        }
        if (links.isNotEmpty()) Spacer(Modifier.height(10.dp))
        Text("Leitura quantitativa", style = MaterialTheme.typography.titleMedium)
        Text("As palavras são contadas diretamente do texto salvo de cada capítulo. A estimativa de páginas usa 250 palavras por página; não representa paginação final, que depende de fonte, margens, espaçamento e formato de publicação.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        chapters.forEachIndexed { index, chapter ->
            val words = Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}]+)?").findAll(chapter.content).count()
            val pages = if (words == 0) 0 else (words + 249) / 250
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}. ${chapter.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("$words palavras · ~$pages pág.", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
        }
        val totalWords = chapters.sumOf { Regex("[\\p{L}\\p{N}]+(?:['’][\\p{L}]+)?").findAll(it.content).count() }
        Text("Total do projeto: $totalWords palavras · ~${if (totalWords == 0) 0 else (totalWords + 249) / 250} páginas estimadas", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall)
    }
}
