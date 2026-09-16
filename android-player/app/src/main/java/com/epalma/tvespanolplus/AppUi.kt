package com.epalma.tvespanolplus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF101114)
private val Panel = Color(0xFF191B20)
private val Panel2 = Color(0xFF23262D)
private val Accent = Color(0xFF19B5A5)
private val Live = Color(0xFFE53935)
private val PrimaryText = Color(0xFFF7F8FA)
private val SecondaryText = Color(0xFFB8BBC3)

@Composable
fun TVEspanolPlusRoot(vm: MainViewModel, onExit: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val screen by vm.screen.collectAsState()
    val filter by vm.filter.collectAsState()
    var confirmExit by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Accent, background = Bg, surface = Panel, onSurface = PrimaryText, onBackground = PrimaryText)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (ui.loading && ui.channels.isEmpty()) {
                Loading(ui.statusMessage)
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (screen != Screen.PLAYER && screen != Screen.DUAL) {
                        Header(ui.refreshing, { vm.go(Screen.HOME) }, vm::openSearch, { vm.refresh(false) }, { vm.go(Screen.SETTINGS) })
                    }
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.HOME -> Home(ui, vm)
                            Screen.CHANNELS -> Channels(ui, filter, vm)
                            Screen.SEARCH -> Search(ui, vm)
                            Screen.PLAYER -> Player(ui, vm)
                            Screen.DUAL -> Dual(ui, vm)
                            Screen.PLAYLISTS -> Playlists(ui, vm)
                            Screen.SETTINGS -> Settings(ui, vm) { confirmExit = true }
                            Screen.ABOUT -> About()
                        }
                        ui.error?.let { ErrorBar(it) { vm.clearError() } }
                    }
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Salir de TV Español+?") },
            text = { Text("Se cerrará la aplicación y se liberarán los reproductores.") },
            confirmButton = { TextButton(onClick = onExit) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(58.dp))
        Spacer(Modifier.width(8.dp))
        Text("TV Español", fontSize = 28.sp, fontWeight = FontWeight.Black, color = PrimaryText, maxLines = 1)
        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Accent)
    }
}

@Composable
private fun Loading(message: String) {
    Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Brand(Modifier.fillMaxWidth(0.62f))
        Spacer(Modifier.height(22.dp)); CircularProgressIndicator(color = Accent); Spacer(Modifier.height(14.dp))
        Text(message, color = SecondaryText); Spacer(Modifier.height(20.dp)); Legal()
    }
}

@Composable
private fun Header(refreshing: Boolean, home: () -> Unit, search: () -> Unit, refresh: () -> Unit, settings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).background(Panel).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Brand(Modifier.width(290.dp).clickable(onClick = home))
        Spacer(Modifier.weight(1f))
        SmallAction("Buscar", Icons.Default.Search, search)
        SmallAction(if (refreshing) "Actualizando" else "Actualizar", Icons.Default.Refresh, refresh, !refreshing)
        SmallAction("Ajustes", Icons.Default.Settings, settings)
    }
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.padding(start = 8.dp)) {
        Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(text)
    }
}

@Composable
private fun Home(ui: AppUiState, vm: MainViewModel) {
    val now = System.currentTimeMillis()
    val liveNow = remember(ui.channels, ui.programs) {
        ui.channels.mapNotNull { c -> c.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(now) }?.let { p -> c to p } } }.take(6)
    }
    val hero = liveNow.firstOrNull()?.first ?: ui.recentIds.firstNotNullOfOrNull { id -> ui.channels.find { it.id == id } } ?: ui.channels.firstOrNull()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth().weight(0.48f)) {
            FocusCard({ hero?.let(vm::play) }, Modifier.weight(1.5f).fillMaxHeight()) {
                Box(Modifier.fillMaxSize()) {
                    hero?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .20f) }
                    Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                        Text("● EN VIVO", color = Live, fontWeight = FontWeight.Bold)
                        Text(liveNow.firstOrNull()?.second?.title ?: hero?.name ?: "TV Español+", fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(hero?.name.orEmpty(), color = SecondaryText); Spacer(Modifier.height(8.dp)); Text("OK para ver", color = Accent)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Ahora", fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                liveNow.take(4).forEach { (c, p) ->
                    FocusCard({ vm.play(c) }, Modifier.fillMaxWidth().height(62.dp).padding(vertical = 3.dp)) {
                        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("●", color = Live); Spacer(Modifier.width(8.dp)); Column { Text(p.title, maxLines = 1); Text(c.name, color = SecondaryText, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp)); Text("Explorar", fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
        val cards = listOf("📺" to "En vivo", "⚽" to "Deportes", "🎬" to "Películas", "📺" to "Series", "✝" to "Cristianos", "🌎" to "Países", "♥" to "Favoritos", "☰" to "Más")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cards) { (icon, title) ->
                FocusCard({ if (title == "Más") vm.go(Screen.SETTINGS) else vm.openCategory(title) }, Modifier.width(170.dp).height(102.dp)) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) { Text(icon, fontSize = 26.sp); Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) }
                }
            }
        }
        ui.lastRefresh?.let { r -> Spacer(Modifier.height(8.dp)); Text(if (r.added + r.removed + r.changed == 0) "✓ Lista al día · ${r.channelCount} canales" else "✓ +${r.added} · -${r.removed} · ${r.changed} modificados", color = SecondaryText, fontSize = 12.sp) }
    }
}

private fun channelFilter(ui: AppUiState, filter: String): List<Channel> = when (TextNormalizer.normalize(filter)) {
    "en vivo" -> ui.channels
    "favoritos" -> ui.channels.filter { it.id in ui.favorites }
    "deportes" -> ui.channels.filter { g(it.group, "deport", "sport") }
    "peliculas" -> ui.channels.filter { g(it.group, "cine", "movie", "pelicula") }
    "series" -> ui.channels.filter { g(it.group, "serie") }
    "cristianos" -> ui.channels.filter { g(it.group, "crist", "relig", "fe") }
    "paises" -> ui.channels.filter { "🌎" in it.group }
    else -> ui.channels
}

private fun g(group: String, vararg terms: String): Boolean {
    val n = TextNormalizer.normalize(group); return terms.any { it in n }
}

@Composable
private fun Channels(ui: AppUiState, filter: String, vm: MainViewModel) {
    val list = remember(ui.channels, ui.favorites, filter) { channelFilter(ui, filter) }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id) { preview = null; delay(1500); preview = focused }
    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(.82f)) {
            Text(filter, fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                preview?.let { ResilientPlayer(it, Modifier.fillMaxSize(), volume = 0f, controls = false) }
                if (preview == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Tv, null, tint = SecondaryText, modifier = Modifier.size(44.dp)); Text("Mantén el foco para previsualizar", color = SecondaryText) }
            }
            focused?.let { Spacer(Modifier.height(8.dp)); Text(it.name, fontWeight = FontWeight.Bold); Text(it.group, color = SecondaryText, fontSize = 12.sp) }
        }
        Spacer(Modifier.width(16.dp))
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1.55f), contentPadding = PaddingValues(3.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(list, key = { it.id }) { c ->
                FocusCard({ vm.play(c) }, Modifier.fillMaxWidth().height(106.dp), onFocus = { focused = c }) {
                    Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(52.dp), contentScale = ContentScale.Fit) else Box(Modifier.size(52.dp).background(Panel2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(c.name.take(2).uppercase(), fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(c.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold); Text(c.group, color = SecondaryText, fontSize = 11.sp, maxLines = 1) }
                        IconButton(onClick = { vm.toggleFavorite(c.id) }) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (c.id in ui.favorites) Live else SecondaryText) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Search(ui: AppUiState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        OutlinedTextField(ui.searchQuery, vm::search, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (ui.searchQuery.isNotBlank()) IconButton({ vm.search("") }) { Icon(Icons.Default.Close, "Limpiar") } }, label = { Text("Súper Búsqueda") }, placeholder = { Text("Ej.: fc barcelos, Fórmula 1, noticias Honduras") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
        Spacer(Modifier.height(12.dp))
        if (ui.searchQuery.length < 2) {
            Text("Busca sin importar mayúsculas, acentos o pequeños errores.", color = SecondaryText); Spacer(Modifier.height(12.dp)); QuickRows(ui, vm)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ui.searchResults, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                    FocusCard({ vm.play(hit.channel) }, Modifier.fillMaxWidth().height(82.dp)) {
                        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            hit.channel.logo?.let { AsyncImage(it, null, Modifier.size(54.dp)) }; Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Text(hit.program?.title ?: hit.channel.name, fontWeight = FontWeight.Bold, maxLines = 1); Text("${hit.channel.name} · ${hit.channel.group}", color = SecondaryText, fontSize = 12.sp, maxLines = 1) }
                            Text(if (hit.temporalBucket == TemporalBucket.LIVE_NOW) "● EN VIVO" else hit.reason.uppercase(), color = if (hit.temporalBucket == TemporalBucket.LIVE_NOW) Live else Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp)); OutlinedButton(onClick = { vm.startDual(hit.channel) }) { Text("▣ Vista doble") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickRows(ui: AppUiState, vm: MainViewModel) {
    val recent = ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }
    val fav = ui.channels.filter { it.id in ui.favorites }.take(10)
    if (recent.isNotEmpty()) { Text("Recientes", fontWeight = FontWeight.Bold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(recent) { c -> Mini(c) { vm.play(c) } } }; Spacer(Modifier.height(12.dp)) }
    if (fav.isNotEmpty()) { Text("Favoritos", fontWeight = FontWeight.Bold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(fav) { c -> Mini(c) { vm.play(c) } } } }
}

@Composable
private fun Mini(c: Channel, click: () -> Unit) {
    FocusCard(click, Modifier.width(180.dp).height(68.dp)) { Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { c.logo?.let { AsyncImage(it, null, Modifier.size(40.dp)) }; Spacer(Modifier.width(7.dp)); Text(c.name, maxLines = 2) } }
}

@Composable
private fun Player(ui: AppUiState, vm: MainViewModel) {
    val c = ui.selectedChannel ?: return
    var error by remember(c.id) { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ResilientPlayer(c, Modifier.fillMaxSize(), 1f, true) { error = it }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.72f)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text(c.group, color = SecondaryText, fontSize = 11.sp) }
            OutlinedButton({ vm.toggleFavorite(c.id) }) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Spacer(Modifier.width(4.dp)); Text("Favorito") }
            Spacer(Modifier.width(7.dp)); OutlinedButton({ vm.startDual(c) }) { Text("▣ Vista doble") }; Spacer(Modifier.width(7.dp)); OutlinedButton({ vm.go(Screen.HOME) }) { Text("Volver") }
        }
        error?.let { Text(it, Modifier.align(Alignment.Center).background(Color.Black.copy(.8f), RoundedCornerShape(8.dp)).padding(15.dp)) }
    }
}

@Composable
private fun Dual(ui: AppUiState, vm: MainViewModel) {
    Row(Modifier.fillMaxSize().background(Color.Black)) {
        DualPane(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f))
        Box(Modifier.width(2.dp).fillMaxHeight().background(Accent))
        DualPane(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f))
    }
}

@Composable
private fun DualPane(side: DualSide, channel: Channel?, ui: AppUiState, audio: Boolean, vm: MainViewModel, modifier: Modifier) {
    var q by remember(side) { mutableStateOf("") }
    var results by remember(side) { mutableStateOf<List<SearchHit>>(emptyList()) }
    LaunchedEffect(q, ui.channels, ui.programs) { delay(220); results = if (q.length >= 2) SearchEngine.search(q, ui.channels, ui.programs, limit = 25) else emptyList() }
    Column(modifier.fillMaxHeight().background(if (audio) Color(0xFF071C1A) else Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(q, { q = it }, Modifier.weight(1f), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text(if (side == DualSide.LEFT) "Buscar canal izquierdo…" else "Buscar canal derecho…") }, singleLine = true)
            IconButton({ vm.setDualAudio(side) }) { Text(if (audio) "🔊" else "🔇", fontSize = 19.sp) }
            IconButton({ vm.fullScreenFromDual(side) }, enabled = channel != null) { Icon(Icons.Default.Fullscreen, "Agrandar") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (channel != null) {
                ResilientPlayer(channel, Modifier.fillMaxSize(), if (audio) 1f else 0f, false)
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(.6f)).padding(8.dp)) { Text(channel.name, fontWeight = FontWeight.Bold); Text(if (audio) "Audio activo" else "Sin audio", color = if (audio) Accent else SecondaryText, fontSize = 11.sp) }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Busca un canal para esta pantalla", color = SecondaryText) }
            if (q.length >= 2) {
                LazyColumn(Modifier.fillMaxSize().background(Color(0xEE101114)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(results, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                        FocusCard({ vm.setDualChannel(side, hit.channel); q = "" }, Modifier.fillMaxWidth().height(59.dp)) { Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(hit.program?.title ?: hit.channel.name, maxLines = 1); Text("${hit.channel.name} · ${hit.reason}", color = SecondaryText, fontSize = 10.sp, maxLines = 1) }; if (hit.temporalBucket == TemporalBucket.LIVE_NOW) Text("●", color = Live) } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton({ vm.swapDual() }, Modifier.weight(1f)) { Icon(Icons.Default.SwapHoriz, null); Text("Cambiar") }
            OutlinedButton({ vm.fullScreenFromDual(side) }, Modifier.weight(1f), enabled = channel != null) { Text("⛶ Agrandar") }
            OutlinedButton({ vm.closeDualSide(side) }, Modifier.weight(1f)) { Icon(Icons.Default.Close, null); Text("Cerrar") }
        }
    }
}

@Composable
private fun Playlists(ui: AppUiState, vm: MainViewModel) {
    var name by remember { mutableStateOf("") }; var url by remember { mutableStateOf("") }; var editId by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize().padding(20.dp)) {
        Column(Modifier.weight(1.35f)) {
            Text("Mis listas", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(ui.playlists, key = { it.id }) { p ->
                FocusCard({ name = p.name; url = p.url; editId = p.id }, Modifier.fillMaxWidth().height(88.dp)) {
                    Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text((if (p.active) "✓ " else "") + p.name, fontWeight = FontWeight.Bold); Text(p.url, color = SecondaryText, fontSize = 10.sp, maxLines = 1); Text(if (p.lastUpdatedEpochMs > 0) "Actualizada ${fmt(p.lastUpdatedEpochMs)}" else "Sin actualización", color = SecondaryText, fontSize = 10.sp) }
                        if (!p.active) OutlinedButton({ vm.activatePlaylist(p.id) }) { Text("Seleccionar") }
                    }
                }
            } }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(if (editId == null) "Agregar lista" else "Editar lista", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp))
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true); Spacer(Modifier.height(7.dp))
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("URL M3U (HTTPS)") }, singleLine = true); Spacer(Modifier.height(9.dp))
            Button({ if (editId == null) vm.addPlaylist(name, url) else vm.editPlaylist(editId!!, name, url); name = ""; url = ""; editId = null }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(" Guardar") }
            if (editId != null) { Spacer(Modifier.height(7.dp)); OutlinedButton({ vm.deletePlaylist(editId!!); name = ""; url = ""; editId = null }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Text(" Eliminar") } }
            Spacer(Modifier.height(7.dp)); OutlinedButton({ vm.refresh(false) }, Modifier.fillMaxWidth(), enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, null); Text(" Actualizar lista activa") }
        }
    }
}

@Composable
private fun Settings(ui: AppUiState, vm: MainViewModel, exit: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(20.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Configuración", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
            Setting("📋", "Mis listas", "Agregar, editar, seleccionar y actualizar") { vm.go(Screen.PLAYLISTS) }
            Setting("↻", "Actualización automática", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) }
            Setting("🔍", "Súper Búsqueda", "Acentos, mayúsculas, fuzzy y contexto temporal") { vm.go(Screen.SEARCH) }
            Setting("ℹ", "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) }
            Setting("🚪", "Salir", "Cerrar la aplicación y liberar recursos", exit)
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(14.dp)).padding(18.dp)) {
            Text("Estado", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp))
            Status("Lista activa", ui.activePlaylist?.name ?: "—"); Status("Canales", ui.channels.size.toString()); Status("EPG", if (ui.programs.isNotEmpty()) "Disponible" else "Sin datos"); Status("Favoritos", ui.favorites.size.toString()); Status("Última actualización", ui.activePlaylist?.lastUpdatedEpochMs?.takeIf { it > 0 }?.let(::fmt) ?: "—")
            Spacer(Modifier.height(18.dp)); Legal()
        }
    }
}

@Composable
private fun Setting(icon: String, title: String, subtitle: String, click: () -> Unit) {
    FocusCard(click, Modifier.fillMaxWidth().height(72.dp).padding(vertical = 3.dp)) { Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 21.sp); Spacer(Modifier.width(10.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = SecondaryText, fontSize = 11.sp) } } }
}

@Composable
private fun Status(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, color = SecondaryText, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun About() {
    Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Brand(Modifier.fillMaxWidth(.55f)); Spacer(Modifier.height(14.dp)); Text("Versión 1.0.0", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Android TV / Google TV · Kotlin · Media3", color = SecondaryText); Text("Sin registros. Sin contraseñas. Solo ver.", color = Accent, modifier = Modifier.padding(top = 7.dp)); Spacer(Modifier.height(20.dp)); Legal()
    }
}

@Composable
private fun Legal() { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("© epalma", color = SecondaryText, fontSize = 14.sp); Text("+504 99461582 - Tegucigalpa - Honduras", color = SecondaryText, fontSize = 13.sp) } }

@Composable
private fun BoxScope.ErrorBar(message: String, close: () -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).padding(12.dp).background(Color(0xEE351717), RoundedCornerShape(9.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f)); IconButton(close) { Icon(Icons.Default.Close, "Cerrar") } }
}

@Composable
private fun FocusCard(click: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val border by animateColorAsState(if (focused) Accent else Color.Transparent, label = "focus")
    Card(
        modifier = modifier.scale(if (focused) 1.025f else 1f).border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(11.dp)).clickable(interactionSource = interaction, indication = null, onClick = click).focusable(interactionSource = interaction),
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(11.dp)
    ) { content() }
}

private fun fmt(epoch: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epoch))
