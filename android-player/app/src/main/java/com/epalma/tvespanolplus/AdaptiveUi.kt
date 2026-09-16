package com.epalma.tvespanolplus

import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

private val ABackground = Color(0xFF101114)
private val APanel = Color(0xFF191B20)
private val APanel2 = Color(0xFF23262D)
private val AAccent = Color(0xFF19B5A5)
private val ALive = Color(0xFFE53935)
private val AText = Color(0xFFF7F8FA)
private val ASecondary = Color(0xFFB8BBC3)

private data class CategorySpec(val symbol: String, val title: String)

private val categorySpecs = listOf(
    CategorySpec("⚽", "Deportes"),
    CategorySpec("🎬", "Películas"),
    CategorySpec("📺", "Series"),
    CategorySpec("✝", "Cristianos"),
    CategorySpec("📰", "Noticias"),
    CategorySpec("👶", "Infantil"),
    CategorySpec("♫", "Música"),
    CategorySpec("◉", "Documentales"),
    CategorySpec("🌎", "Países"),
    CategorySpec("♥", "Favoritos")
)

@Composable
fun AdaptiveTVEspanolPlusRoot(vm: MainViewModel, onExit: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val screen by vm.screen.collectAsState()
    val filter by vm.filter.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTv = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val phone = !isTv
    val portrait = configuration.screenHeightDp >= configuration.screenWidthDp
    var confirmExit by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = AAccent,
            background = ABackground,
            surface = APanel,
            onSurface = AText,
            onBackground = AText
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = ABackground) {
            if (ui.loading && ui.channels.isEmpty()) {
                AdaptiveLoading(ui.statusMessage, phone)
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (screen != Screen.PLAYER && screen != Screen.DUAL) {
                        AdaptiveHeader(
                            phone = phone,
                            refreshing = ui.refreshing,
                            home = { vm.go(Screen.HOME) },
                            search = vm::openSearch,
                            refresh = { vm.refresh(false) },
                            settings = { vm.go(Screen.SETTINGS) }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.HOME -> AdaptiveHome(ui, vm, phone, portrait)
                            Screen.CHANNELS -> AdaptiveChannels(ui, filter, vm, phone)
                            Screen.SEARCH -> AdaptiveSearch(ui, vm, phone)
                            Screen.PLAYER -> AdaptivePlayer(ui, vm, phone, portrait)
                            Screen.DUAL -> AdaptiveDual(ui, vm, phone, portrait)
                            Screen.PLAYLISTS -> AdaptivePlaylists(ui, vm, phone)
                            Screen.SETTINGS -> AdaptiveSettings(ui, vm, phone) { confirmExit = true }
                            Screen.ABOUT -> AdaptiveAbout(phone)
                        }
                        ui.error?.let { AdaptiveErrorBar(it) { vm.clearError() } }
                    }
                    if (phone && screen != Screen.PLAYER && screen != Screen.DUAL) {
                        MobileBottomBar(vm, screen)
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
private fun AdaptiveBrand(phone: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_tv_espanol),
            contentDescription = "TV Español+",
            modifier = Modifier.size(if (phone) 38.dp else 54.dp)
        )
        Spacer(Modifier.width(if (phone) 5.dp else 8.dp))
        Text("TV Español", fontSize = if (phone) 19.sp else 27.sp, fontWeight = FontWeight.Black, color = AText, maxLines = 1)
        Text("+", fontSize = if (phone) 22.sp else 31.sp, fontWeight = FontWeight.Black, color = AAccent)
    }
}

@Composable
private fun AdaptiveLoading(message: String, phone: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(if (phone) 22.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AdaptiveBrand(phone)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(color = AAccent)
        Spacer(Modifier.height(12.dp))
        Text(message, color = ASecondary)
        Spacer(Modifier.height(18.dp))
        AdaptiveLegal()
    }
}

@Composable
private fun AdaptiveHeader(
    phone: Boolean,
    refreshing: Boolean,
    home: () -> Unit,
    search: () -> Unit,
    refresh: () -> Unit,
    settings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(if (phone) 58.dp else 72.dp).background(APanel).padding(horizontal = if (phone) 8.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveBrand(phone, Modifier.clickable(onClick = home))
        Spacer(Modifier.weight(1f))
        if (phone) {
            IconButton(search) { Icon(Icons.Default.Search, "Buscar") }
            IconButton(refresh, enabled = !refreshing) { Icon(Icons.Default.Refresh, if (refreshing) "Actualizando" else "Actualizar") }
            IconButton(settings) { Icon(Icons.Default.Settings, "Ajustes") }
        } else {
            AdaptiveAction("Buscar", Icons.Default.Search, search)
            AdaptiveAction(if (refreshing) "Actualizando" else "Actualizar", Icons.Default.Refresh, refresh, !refreshing)
            AdaptiveAction("Ajustes", Icons.Default.Settings, settings)
        }
    }
}

@Composable
private fun AdaptiveAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.padding(start = 8.dp)) {
        Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(text)
    }
}

@Composable
private fun MobileBottomBar(vm: MainViewModel, screen: Screen) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).background(APanel).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomAction("Inicio", Icons.Default.Home, screen == Screen.HOME) { vm.go(Screen.HOME) }
        BottomAction("Buscar", Icons.Default.Search, screen == Screen.SEARCH) { vm.openSearch() }
        BottomAction("Favoritos", Icons.Default.Favorite, false) { vm.openCategory("Favoritos") }
        BottomAction("Más", Icons.Default.Settings, screen == Screen.SETTINGS) { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun BottomAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, action: () -> Unit) {
    TextButton(onClick = action) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = if (selected) AAccent else ASecondary, modifier = Modifier.size(20.dp))
            Text(label, color = if (selected) AAccent else ASecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdaptiveHome(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val now = System.currentTimeMillis()
    val liveNow = remember(ui.channels, ui.programs, now / 60_000L) {
        ui.channels.mapNotNull { c ->
            c.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(now) }?.let { p -> c to p } }
        }.take(if (phone) 8 else 6)
    }
    val recent = remember(ui.recentIds, ui.channels) { ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }.take(8) }
    val hero = liveNow.firstOrNull()?.first ?: recent.firstOrNull() ?: ui.channels.firstOrNull()

    if (phone && portrait) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                AdaptiveHero(hero, liveNow.firstOrNull()?.second?.title, vm, Modifier.fillMaxWidth().height(190.dp), phone = true)
            }
            item { SectionTitle("Ahora en vivo", "Lo más cercano a la hora actual") }
            items(liveNow, key = { it.first.id }) { (channel, program) ->
                LiveRow(channel, program.title) { vm.play(channel) }
            }
            if (recent.isNotEmpty()) {
                item { SectionTitle("Continúa viendo", "Acceso rápido a tus últimos canales") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent, key = { it.id }) { c -> AdaptiveMini(c) { vm.play(c) } }
                    }
                }
            }
            item { SectionTitle("Categorías", "En vivo es un estado, no una categoría duplicada") }
            items(categorySpecs.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { spec ->
                        CategoryCard(spec, vm, Modifier.weight(1f).height(86.dp), phone = true)
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { RefreshSummary(ui) }
            item { Spacer(Modifier.height(4.dp)) }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(if (phone) 12.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth().weight(0.52f)) {
                AdaptiveHero(hero, liveNow.firstOrNull()?.second?.title, vm, Modifier.weight(1.45f).fillMaxHeight(), phone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SectionTitle("Ahora en vivo", "Ordenado por contexto temporal")
                    liveNow.take(4).forEach { (channel, program) ->
                        LiveRow(channel, program.title) { vm.play(channel) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionTitle("Categorías", "Sin repetir “En vivo”")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorySpecs) { spec -> CategoryCard(spec, vm, Modifier.width(if (phone) 145.dp else 165.dp).height(92.dp), phone) }
            }
            RefreshSummary(ui)
        }
    }
}

@Composable
private fun AdaptiveHero(channel: Channel?, programTitle: String?, vm: MainViewModel, modifier: Modifier, phone: Boolean) {
    AdaptiveFocusCard({ channel?.let(vm::play) }, modifier) {
        Box(Modifier.fillMaxSize()) {
            channel?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .18f) }
            Column(Modifier.align(Alignment.BottomStart).padding(if (phone) 14.dp else 20.dp)) {
                Text("● AHORA EN VIVO", color = ALive, fontWeight = FontWeight.Bold, fontSize = if (phone) 12.sp else 14.sp)
                Text(programTitle ?: channel?.name ?: "TV Español+", fontSize = if (phone) 22.sp else 28.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(channel?.name.orEmpty(), color = ASecondary, fontSize = if (phone) 12.sp else 14.sp)
                Spacer(Modifier.height(5.dp))
                Text(if (phone) "Toca para ver" else "OK para ver", color = AAccent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = ASecondary, fontSize = 11.sp) }
    }
}

@Composable
private fun LiveRow(channel: Channel, title: String, click: () -> Unit) {
    AdaptiveFocusCard(click, Modifier.fillMaxWidth().height(60.dp).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = ALive)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(channel.name, color = ASecondary, fontSize = 10.sp, maxLines = 1)
            }
            Text("VER", color = AAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryCard(spec: CategorySpec, vm: MainViewModel, modifier: Modifier, phone: Boolean) {
    AdaptiveFocusCard({ vm.openCategory(spec.title) }, modifier) {
        Column(Modifier.fillMaxSize().padding(if (phone) 11.dp else 13.dp), verticalArrangement = Arrangement.Center) {
            Text(spec.symbol, fontSize = if (phone) 23.sp else 26.sp)
            Text(spec.title, fontWeight = FontWeight.SemiBold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1)
        }
    }
}

@Composable
private fun RefreshSummary(ui: AppUiState) {
    ui.lastRefresh?.let { r ->
        Spacer(Modifier.height(6.dp))
        Text(
            if (r.added + r.removed + r.changed == 0) "✓ Lista al día · ${r.channelCount} canales"
            else "✓ +${r.added} · -${r.removed} · ${r.changed} modificados",
            color = ASecondary,
            fontSize = 11.sp
        )
    }
}

private fun adaptiveFilter(ui: AppUiState, filter: String): List<Channel> = when (TextNormalizer.normalize(filter)) {
    "favoritos" -> ui.channels.filter { it.id in ui.favorites }
    "deportes" -> ui.channels.filter { ag(it.group, "deport", "sport") }
    "peliculas" -> ui.channels.filter { ag(it.group, "cine", "movie", "pelicula") }
    "series" -> ui.channels.filter { ag(it.group, "serie", "telenov") }
    "cristianos" -> ui.channels.filter { ag(it.group, "crist", "relig", "fe", "catolic") }
    "noticias" -> ui.channels.filter { ag(it.group, "notic", "news") }
    "infantil" -> ui.channels.filter { ag(it.group, "infantil", "kids", "nino", "familia") }
    "musica" -> ui.channels.filter { ag(it.group, "music") }
    "documentales" -> ui.channels.filter { ag(it.group, "document", "cultura") }
    "paises" -> ui.channels.filter { it.country != null || "🌎" in it.group }
    else -> ui.channels
}

private fun ag(group: String, vararg terms: String): Boolean {
    val n = TextNormalizer.normalize(group)
    return terms.any { it in n }
}

@Composable
private fun AdaptiveChannels(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean) {
    val list = remember(ui.channels, ui.favorites, filter) { adaptiveFilter(ui, filter) }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id, phone) {
        preview = null
        if (!phone) {
            delay(1200)
            preview = focused
        }
    }

    if (phone) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(filter, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${list.size} canales", color = ASecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { c -> ChannelCard(c, ui, vm, Modifier.fillMaxWidth().height(112.dp), phone = true) { focused = c } }
            }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.weight(.82f)) {
                Text(filter, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    preview?.let { ResilientPlayer(it, Modifier.fillMaxSize(), volume = 0f, controls = false) }
                    if (preview == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tv, null, tint = ASecondary, modifier = Modifier.size(42.dp))
                        Text("Mantén el foco para previsualizar", color = ASecondary)
                    }
                }
                focused?.let {
                    Spacer(Modifier.height(7.dp)); Text(it.name, fontWeight = FontWeight.Bold); Text(it.group, color = ASecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            LazyVerticalGrid(
                GridCells.Fixed(3),
                Modifier.weight(1.55f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { c -> ChannelCard(c, ui, vm, Modifier.fillMaxWidth().height(104.dp), phone = false) { focused = c } }
            }
        }
    }
}

@Composable
private fun ChannelCard(c: Channel, ui: AppUiState, vm: MainViewModel, modifier: Modifier, phone: Boolean, onFocus: () -> Unit) {
    AdaptiveFocusCard({ vm.play(c) }, modifier, onFocus) {
        Column(Modifier.fillMaxSize().padding(if (phone) 8.dp else 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(if (phone) 42.dp else 49.dp), contentScale = ContentScale.Fit)
                else Box(Modifier.size(if (phone) 42.dp else 49.dp).background(APanel2, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text(c.name.take(2).uppercase(), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(7.dp))
                Text(c.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = if (phone) 12.sp else 14.sp)
                IconButton(onClick = { vm.toggleFavorite(c.id) }, modifier = Modifier.size(34.dp)) {
                    Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (c.id in ui.favorites) ALive else ASecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(c.group, color = ASecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AdaptiveSearch(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        OutlinedTextField(
            value = ui.searchQuery,
            onValueChange = vm::search,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (ui.searchQuery.isNotBlank()) IconButton({ vm.search("") }) { Icon(Icons.Default.Close, "Limpiar") } },
            label = { Text("Súper Búsqueda") },
            placeholder = { Text("fc barcelos, Fórmula 1, noticias Honduras") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(Modifier.height(8.dp))
        if (ui.searchQuery.length < 2) {
            Text("Ignora mayúsculas, acentos y tolera pequeños errores. Prioriza lo más cercano a la fecha y hora actuales.", color = ASecondary, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            AdaptiveQuickRows(ui, vm)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ui.searchResults, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                    AdaptiveFocusCard({ vm.play(hit.channel) }, Modifier.fillMaxWidth().height(if (phone) 78.dp else 82.dp)) {
                        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            hit.channel.logo?.let { AsyncImage(it, null, Modifier.size(if (phone) 42.dp else 52.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(hit.program?.title ?: hit.channel.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${hit.channel.name} · ${hit.channel.group}", color = ASecondary, fontSize = 10.sp, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (hit.temporalBucket == TemporalBucket.LIVE_NOW) "● EN VIVO" else hit.reason.uppercase(), color = if (hit.temporalBucket == TemporalBucket.LIVE_NOW) ALive else AAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { vm.startDual(hit.channel) }) { Text("▣ Dual", fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveQuickRows(ui: AppUiState, vm: MainViewModel) {
    val recent = ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }.take(10)
    val fav = ui.channels.filter { it.id in ui.favorites }.take(10)
    if (recent.isNotEmpty()) {
        Text("Recientes", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(recent, key = { it.id }) { c -> AdaptiveMini(c) { vm.play(c) } } }
        Spacer(Modifier.height(10.dp))
    }
    if (fav.isNotEmpty()) {
        Text("Favoritos", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(fav, key = { it.id }) { c -> AdaptiveMini(c) { vm.play(c) } } }
    }
}

@Composable
private fun AdaptiveMini(c: Channel, click: () -> Unit) {
    AdaptiveFocusCard(click, Modifier.width(160.dp).height(64.dp)) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            c.logo?.let { AsyncImage(it, null, Modifier.size(36.dp)) }
            Spacer(Modifier.width(6.dp)); Text(c.name, maxLines = 2, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdaptivePlayer(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val c = ui.selectedChannel ?: return
    var error by remember(c.id) { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ResilientPlayer(c, Modifier.fillMaxSize(), 1f, true) { error = it }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.76f)).padding(if (phone) 7.dp else 9.dp)) {
            Text(c.name, fontWeight = FontWeight.Bold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1)
            Text(c.group, color = ASecondary, fontSize = 10.sp, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton({ vm.toggleFavorite(c.id) }, Modifier.weight(1f)) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(17.dp)); if (!phone || !portrait) Text(" Favorito") }
                OutlinedButton({ vm.startDual(c) }, Modifier.weight(1f)) { Text("▣${if (!phone || !portrait) " Vista doble" else " Dual"}") }
                OutlinedButton({ vm.go(Screen.HOME) }, Modifier.weight(1f)) { Text("Volver") }
            }
        }
        error?.let { Text(it, Modifier.align(Alignment.Center).background(Color.Black.copy(.82f), RoundedCornerShape(8.dp)).padding(14.dp)) }
    }
}

@Composable
private fun AdaptiveDual(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    if (phone && portrait) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            AdaptiveDualPane(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), compact = true)
            Box(Modifier.fillMaxWidth().height(2.dp).background(AAccent))
            AdaptiveDualPane(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), compact = true)
        }
    } else {
        Row(Modifier.fillMaxSize().background(Color.Black)) {
            AdaptiveDualPane(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), compact = phone)
            Box(Modifier.width(2.dp).fillMaxHeight().background(AAccent))
            AdaptiveDualPane(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), compact = phone)
        }
    }
}

@Composable
private fun AdaptiveDualPane(side: DualSide, channel: Channel?, ui: AppUiState, audio: Boolean, vm: MainViewModel, modifier: Modifier, compact: Boolean) {
    var q by remember(side) { mutableStateOf("") }
    var results by remember(side) { mutableStateOf<List<SearchHit>>(emptyList()) }
    LaunchedEffect(q, ui.channels, ui.programs) {
        delay(220)
        results = if (q.length >= 2) SearchEngine.search(q, ui.channels, ui.programs, limit = 25) else emptyList()
    }
    Column(modifier.fillMaxHeight().background(if (audio) Color(0xFF071C1A) else Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(if (compact) 3.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                q,
                { q = it },
                Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                placeholder = { Text(if (side == DualSide.LEFT) "Buscar izquierda…" else "Buscar derecha…", fontSize = if (compact) 10.sp else 12.sp) },
                singleLine = true
            )
            IconButton({ vm.setDualAudio(side) }, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) { Text(if (audio) "🔊" else "🔇", fontSize = 16.sp) }
            IconButton({ vm.fullScreenFromDual(side) }, enabled = channel != null, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) { Icon(Icons.Default.Fullscreen, "Agrandar") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (channel != null) {
                ResilientPlayer(channel, Modifier.fillMaxSize(), if (audio) 1f else 0f, false)
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(.65f)).padding(5.dp)) {
                    Text(channel.name, fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(if (audio) "AUDIO" else "MUDO", color = if (audio) AAccent else ASecondary, fontSize = 8.sp)
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Busca un canal", color = ASecondary, fontSize = 11.sp) }
            if (q.length >= 2) {
                LazyColumn(Modifier.fillMaxSize().background(Color(0xEE101114)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                        AdaptiveFocusCard({ vm.setDualChannel(side, hit.channel); q = "" }, Modifier.fillMaxWidth().height(if (compact) 48.dp else 56.dp)) {
                            Row(Modifier.fillMaxSize().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(hit.program?.title ?: hit.channel.name, maxLines = 1, fontSize = if (compact) 9.sp else 11.sp)
                                    Text("${hit.channel.name} · ${hit.reason}", color = ASecondary, fontSize = if (compact) 7.sp else 9.sp, maxLines = 1)
                                }
                                if (hit.temporalBucket == TemporalBucket.LIVE_NOW) Text("●", color = ALive)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            IconButton({ vm.swapDual() }, Modifier.weight(1f)) { Icon(Icons.Default.SwapHoriz, "Intercambiar") }
            IconButton({ vm.fullScreenFromDual(side) }, Modifier.weight(1f), enabled = channel != null) { Icon(Icons.Default.Fullscreen, "Agrandar") }
            IconButton({ vm.closeDualSide(side) }, Modifier.weight(1f)) { Icon(Icons.Default.Close, "Cerrar") }
        }
    }
}

@Composable
private fun AdaptivePlaylists(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }

    if (phone) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            item { Text("Mis listas", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            items(ui.playlists, key = { it.id }) { p ->
                PlaylistCard(p, vm, { name = p.name; url = p.url; editId = p.id }, phone = true)
            }
            item { PlaylistEditor(name, url, editId, ui, vm, { name = it }, { url = it }) { name = ""; url = ""; editId = null } }
            item { AdaptiveLegal() }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.weight(1.35f)) {
                Text("Mis listas", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ui.playlists, key = { it.id }) { p -> PlaylistCard(p, vm, { name = p.name; url = p.url; editId = p.id }, phone = false) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { PlaylistEditor(name, url, editId, ui, vm, { name = it }, { url = it }) { name = ""; url = ""; editId = null } }
        }
    }
}

@Composable
private fun PlaylistCard(p: PlaylistConfig, vm: MainViewModel, edit: () -> Unit, phone: Boolean) {
    AdaptiveFocusCard(edit, Modifier.fillMaxWidth().height(if (phone) 96.dp else 86.dp)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (p.active) "✓ " else "") + p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(p.url, color = ASecondary, fontSize = 9.sp, maxLines = 1)
                Text(if (p.lastUpdatedEpochMs > 0) "Actualizada ${adaptiveFmt(p.lastUpdatedEpochMs)}" else "Sin actualización", color = ASecondary, fontSize = 9.sp)
            }
            if (!p.active) TextButton({ vm.activatePlaylist(p.id) }) { Text("Usar") }
        }
    }
}

@Composable
private fun PlaylistEditor(name: String, url: String, editId: String?, ui: AppUiState, vm: MainViewModel, setName: (String) -> Unit, setUrl: (String) -> Unit, clear: () -> Unit) {
    Column {
        Text(if (editId == null) "Agregar lista" else "Editar lista", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(name, setName, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(url, setUrl, Modifier.fillMaxWidth(), label = { Text("URL M3U") }, singleLine = true)
        Spacer(Modifier.height(7.dp))
        Button({
            if (editId == null) vm.addPlaylist(name, url) else vm.editPlaylist(editId, name, url)
            clear()
        }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(" Guardar") }
        if (editId != null) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton({ vm.deletePlaylist(editId); clear() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Text(" Eliminar") }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton({ vm.refresh(false) }, Modifier.fillMaxWidth(), enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, null); Text(" Actualizar lista activa") }
    }
}

@Composable
private fun AdaptiveSettings(ui: AppUiState, vm: MainViewModel, phone: Boolean, exit: () -> Unit) {
    if (phone) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Configuración", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            item { AdaptiveSetting("📋", "Mis listas", "Agregar, editar, seleccionar y actualizar") { vm.go(Screen.PLAYLISTS) } }
            item { AdaptiveSetting("↻", "Actualización automática", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) } }
            item { AdaptiveSetting("🔍", "Súper Búsqueda", "Acentos, fuzzy y prioridad por fecha/hora") { vm.go(Screen.SEARCH) } }
            item { AdaptiveSetting("▣", "Vista doble", "Dos canales, dos buscadores, un audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) } }
            item { AdaptiveSetting("ℹ", "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) } }
            item { AdaptiveSetting("🚪", "Salir", "Cerrar la aplicación y liberar recursos", exit) }
            item { AdaptiveStatusPanel(ui) }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Configuración", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
                AdaptiveSetting("📋", "Mis listas", "Agregar, editar, seleccionar y actualizar") { vm.go(Screen.PLAYLISTS) }
                AdaptiveSetting("↻", "Actualización automática", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) }
                AdaptiveSetting("🔍", "Súper Búsqueda", "Acentos, fuzzy y prioridad por fecha/hora") { vm.go(Screen.SEARCH) }
                AdaptiveSetting("▣", "Vista doble", "Dos canales, dos buscadores, un audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) }
                AdaptiveSetting("ℹ", "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) }
                AdaptiveSetting("🚪", "Salir", "Cerrar la aplicación y liberar recursos", exit)
            }
            Spacer(Modifier.width(22.dp))
            AdaptiveStatusPanel(ui, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdaptiveSetting(icon: String, title: String, subtitle: String, click: () -> Unit) {
    AdaptiveFocusCard(click, Modifier.fillMaxWidth().height(68.dp).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp); Spacer(Modifier.width(9.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = ASecondary, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun AdaptiveStatusPanel(ui: AppUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(APanel, RoundedCornerShape(13.dp)).padding(15.dp)) {
        Text("Estado", fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp))
        AdaptiveStatus("Lista activa", ui.activePlaylist?.name ?: "—")
        AdaptiveStatus("Canales", ui.channels.size.toString())
        AdaptiveStatus("EPG", if (ui.programs.isNotEmpty()) "Disponible" else "Sin datos")
        AdaptiveStatus("Favoritos", ui.favorites.size.toString())
        AdaptiveStatus("Última actualización", ui.activePlaylist?.lastUpdatedEpochMs?.takeIf { it > 0 }?.let(::adaptiveFmt) ?: "—")
        Spacer(Modifier.height(14.dp)); AdaptiveLegal()
    }
}

@Composable
private fun AdaptiveStatus(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label, color = ASecondary, modifier = Modifier.weight(1f), fontSize = 11.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) }
}

@Composable
private fun AdaptiveAbout(phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 20.dp else 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AdaptiveBrand(phone)
        Spacer(Modifier.height(12.dp))
        Text("Versión 1.1.0 BETA", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Android TV · Google TV · Android móvil · Kotlin · Media3", color = ASecondary, fontSize = 11.sp)
        Text("Sin registro. Sin contraseña. M3U + EPG + Súper Búsqueda + Vista doble.", color = AAccent, modifier = Modifier.padding(top = 7.dp), fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        AdaptiveLegal()
    }
}

@Composable
private fun AdaptiveLegal() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("© epalma", color = ASecondary, fontSize = 13.sp)
        Text("+504 99461582 - Tegucigalpa - Honduras", color = ASecondary, fontSize = 12.sp)
    }
}

@Composable
private fun BoxScope.AdaptiveErrorBar(message: String, close: () -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).padding(10.dp).background(Color(0xEE351717), RoundedCornerShape(9.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), fontSize = 11.sp); IconButton(close) { Icon(Icons.Default.Close, "Cerrar") }
    }
}

@Composable
private fun AdaptiveFocusCard(click: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val border by animateColorAsState(if (focused) AAccent else Color.Transparent, label = "adaptive-focus")
    Card(
        modifier = modifier
            .scale(if (focused) 1.02f else 1f)
            .border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = click)
            .focusable(interactionSource = interaction),
        colors = CardDefaults.cardColors(containerColor = APanel),
        shape = RoundedCornerShape(10.dp)
    ) { content() }
}

private fun adaptiveFmt(epoch: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epoch))
