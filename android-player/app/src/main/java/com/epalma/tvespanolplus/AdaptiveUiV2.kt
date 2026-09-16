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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TheaterComedy
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private val V2Bg = Color(0xFF101114)
private val V2Panel = Color(0xFF191B20)
private val V2Panel2 = Color(0xFF23262D)
private val V2Accent = Color(0xFF19B5A5)
private val V2Live = Color(0xFFE53935)
private val V2Text = Color(0xFFF7F8FA)
private val V2Secondary = Color(0xFFB8BBC3)

@Composable
fun TVEspanolPlusRootV2(vm: MainViewModel, onExit: () -> Unit) {
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
            primary = V2Accent,
            background = V2Bg,
            surface = V2Panel,
            onSurface = V2Text,
            onBackground = V2Text
        )
    ) {
        Surface(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = V2Bg
        ) {
            if (ui.loading && ui.channels.isEmpty()) {
                V2Loading(ui.statusMessage, phone)
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (screen != Screen.PLAYER && screen != Screen.DUAL) {
                        V2Header(
                            phone = phone,
                            refreshing = ui.refreshing,
                            home = { vm.go(Screen.HOME) },
                            search = vm::openSearch,
                            refresh = { vm.refresh(false) },
                            settings = { vm.go(Screen.SETTINGS) },
                            exit = { confirmExit = true }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.HOME -> V2Home(ui, vm, phone, portrait)
                            Screen.CHANNELS -> V2Channels(ui, filter, vm, phone)
                            Screen.SEARCH -> V2Search(ui, vm, phone)
                            Screen.PLAYER -> V2Player(ui, vm, phone, portrait)
                            Screen.DUAL -> V2Dual(ui, vm, phone, portrait)
                            Screen.PLAYLISTS -> V2Playlists(ui, vm, phone)
                            Screen.SETTINGS -> V2Settings(ui, vm, phone)
                            Screen.ABOUT -> V2About(phone)
                        }
                        ui.error?.let { V2ErrorBar(it) { vm.clearError() } }
                    }
                    if (phone && screen != Screen.PLAYER && screen != Screen.DUAL) {
                        V2BottomBar(vm, screen)
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
private fun V2Brand(phone: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_tv_espanol),
            contentDescription = "TV Español+",
            modifier = Modifier.size(if (phone) 34.dp else 52.dp)
        )
        Spacer(Modifier.width(if (phone) 4.dp else 7.dp))
        Text("TV Español", fontSize = if (phone) 17.sp else 27.sp, fontWeight = FontWeight.Black, color = V2Text, maxLines = 1)
        Text("+", fontSize = if (phone) 20.sp else 31.sp, fontWeight = FontWeight.Black, color = V2Accent)
    }
}

@Composable
private fun V2Loading(message: String, phone: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        V2Brand(phone)
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = V2Accent)
        Spacer(Modifier.height(12.dp))
        Text(message, color = V2Secondary)
        Spacer(Modifier.height(18.dp))
        V2Legal()
    }
}

@Composable
private fun V2Header(
    phone: Boolean,
    refreshing: Boolean,
    home: () -> Unit,
    search: () -> Unit,
    refresh: () -> Unit,
    settings: () -> Unit,
    exit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(if (phone) 56.dp else 72.dp).background(V2Panel).padding(horizontal = if (phone) 6.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V2Brand(phone, Modifier.clickable(onClick = home))
        Spacer(Modifier.weight(1f))
        if (phone) {
            V2TopIcon(Icons.Default.Search, "Buscar", search)
            V2TopIcon(Icons.Default.Refresh, if (refreshing) "Actualizando" else "Actualizar", refresh, !refreshing)
            V2TopIcon(Icons.Default.Settings, "Configuración", settings)
            V2TopIcon(Icons.Default.ExitToApp, "Salir", exit)
        } else {
            V2HeaderButton("Buscar", Icons.Default.Search, search)
            V2HeaderButton(if (refreshing) "Actualizando" else "Actualizar", Icons.Default.Refresh, refresh, !refreshing)
            V2HeaderButton("Configuración", Icons.Default.Settings, settings)
            V2HeaderButton("Salir", Icons.Default.ExitToApp, exit)
        }
    }
}

@Composable
private fun V2TopIcon(icon: ImageVector, description: String, action: () -> Unit, enabled: Boolean = true) {
    IconButton(action, enabled = enabled, modifier = Modifier.size(38.dp)) {
        Icon(icon, description, Modifier.size(22.dp))
    }
}

@Composable
private fun V2HeaderButton(text: String, icon: ImageVector, action: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.padding(start = 7.dp)) {
        Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(text)
    }
}

@Composable
private fun V2BottomBar(vm: MainViewModel, screen: Screen) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(V2Panel).padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        V2BottomAction("Inicio", Icons.Default.Home, screen == Screen.HOME) { vm.go(Screen.HOME) }
        V2BottomAction("Buscar", Icons.Default.Search, screen == Screen.SEARCH) { vm.openSearch() }
        V2BottomAction("Favoritos", Icons.Default.Favorite, false) { vm.openCategory("Favoritos") }
        V2BottomAction("Más", Icons.Default.Settings, screen == Screen.SETTINGS) { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun V2BottomAction(label: String, icon: ImageVector, selected: Boolean, action: () -> Unit) {
    TextButton(onClick = action) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = if (selected) V2Accent else V2Secondary, modifier = Modifier.size(20.dp))
            Text(label, color = if (selected) V2Accent else V2Secondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V2Home(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val now = System.currentTimeMillis()
    val liveNow = remember(ui.channels, ui.programs, now / 60_000L) {
        ui.channels.mapNotNull { c ->
            c.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(now) }?.let { p -> c to p } }
        }.distinctBy { it.first.id }.take(if (phone) 8 else 6)
    }
    val recent = remember(ui.recentIds, ui.channels) { ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }.take(8) }
    val heroPair = liveNow.firstOrNull()
    val hero = heroPair?.first ?: recent.firstOrNull() ?: ui.channels.firstOrNull()
    val heroProgram = heroPair?.second
    val counts = remember(ui.channels) { ChannelClassifier.categoryCounts(ui.channels) }
    val countries = remember(ui.channels) { ChannelClassifier.countries(ui.channels) }

    if (phone && portrait) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(2.dp)) }
            item { V2Hero(hero, heroProgram, vm, Modifier.fillMaxWidth().height(190.dp), phone = true) }
            if (liveNow.isNotEmpty()) {
                item { V2SectionTitle("Ahora en vivo", "Lo más relevante cerca de la hora actual") }
                items(liveNow, key = { it.first.id }) { (channel, program) -> V2LiveRow(channel, program) { vm.play(channel) } }
            }
            if (recent.isNotEmpty()) {
                item { V2SectionTitle("Continúa viendo", "Tus últimos canales") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent, key = { it.id }) { c -> V2Mini(c) { vm.play(c) } }
                    }
                }
            }
            item { V2SectionTitle("Categorías", "Cada canal pertenece a una sola categoría") }
            val visible = ChannelClassifier.categories.filter { (counts[it.title] ?: 0) > 0 }
            items(visible.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { spec ->
                        V2CategoryCard(spec.title, counts[spec.title] ?: 0, vm, Modifier.weight(1f).height(92.dp), true)
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V2SpecialCard("Países", "${countries.size} países", Icons.Default.Public, { vm.openCategory("Países") }, Modifier.weight(1f).height(92.dp), true)
                    V2SpecialCard("Favoritos", "${ui.favorites.size} canales", Icons.Default.Favorite, { vm.openCategory("Favoritos") }, Modifier.weight(1f).height(92.dp), true)
                }
            }
            item { V2RefreshSummary(ui) }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(if (phone) 12.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth().weight(0.52f)) {
                V2Hero(hero, heroProgram, vm, Modifier.weight(1.4f).fillMaxHeight(), phone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    V2SectionTitle("Ahora en vivo", "Prioridad por fecha y hora")
                    liveNow.take(4).forEach { (channel, program) -> V2LiveRow(channel, program) { vm.play(channel) } }
                }
            }
            Spacer(Modifier.height(12.dp))
            V2SectionTitle("Categorías", "Sin canales repetidos entre categorías")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChannelClassifier.categories.filter { (counts[it.title] ?: 0) > 0 }) { spec ->
                    V2CategoryCard(spec.title, counts[spec.title] ?: 0, vm, Modifier.width(170.dp).height(94.dp), phone)
                }
                item { V2SpecialCard("Países", "${countries.size} países", Icons.Default.Public, { vm.openCategory("Países") }, Modifier.width(170.dp).height(94.dp), phone) }
                item { V2SpecialCard("Favoritos", "${ui.favorites.size} canales", Icons.Default.Favorite, { vm.openCategory("Favoritos") }, Modifier.width(170.dp).height(94.dp), phone) }
            }
            V2RefreshSummary(ui)
        }
    }
}

@Composable
private fun V2Hero(channel: Channel?, program: Program?, vm: MainViewModel, modifier: Modifier, phone: Boolean) {
    V2FocusCard({ channel?.let(vm::play) }, modifier) {
        Box(Modifier.fillMaxSize()) {
            channel?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .18f) }
            Column(Modifier.align(Alignment.BottomStart).padding(if (phone) 14.dp else 20.dp)) {
                val name = channel?.let(::cleanChannelName).orEmpty()
                val title = program?.title?.takeIf { TextNormalizer.normalize(it) != TextNormalizer.normalize(name) } ?: name.ifBlank { "TV Español+" }
                Text(title, fontSize = if (phone) 24.sp else 30.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                if (program != null) {
                    Text("● EN VIVO", color = V2Live, fontWeight = FontWeight.Bold, fontSize = if (phone) 11.sp else 13.sp)
                    Text(name, color = V2Secondary, fontSize = if (phone) 12.sp else 14.sp)
                } else {
                    Text(channel?.group.orEmpty(), color = V2Secondary, fontSize = if (phone) 11.sp else 13.sp, maxLines = 1)
                }
                qualityFromName(channel?.name.orEmpty())?.let { Text(it, color = V2Secondary, fontSize = 10.sp) }
                Spacer(Modifier.height(6.dp))
                Text(if (phone) "Toca para ver" else "OK para ver", color = V2Accent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V2SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = V2Secondary, fontSize = 11.sp) }
    }
}

@Composable
private fun V2LiveRow(channel: Channel, program: Program, click: () -> Unit) {
    V2FocusCard(click, Modifier.fillMaxWidth().height(62.dp).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(program.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text("● EN VIVO · ${cleanChannelName(channel)}", color = V2Secondary, fontSize = 10.sp, maxLines = 1)
            }
            Text("VER", color = V2Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V2CategoryCard(title: String, count: Int, vm: MainViewModel, modifier: Modifier, phone: Boolean) {
    V2SpecialCard(title, "$count canales", V2Icons.forCategory(title), { vm.openCategory(title) }, modifier, phone)
}

@Composable
private fun V2SpecialCard(title: String, subtitle: String, icon: ImageVector, click: () -> Unit, modifier: Modifier, phone: Boolean) {
    V2FocusCard(click, modifier) {
        Column(Modifier.fillMaxSize().padding(if (phone) 11.dp else 13.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = V2Accent, modifier = Modifier.size(if (phone) 24.dp else 28.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = V2Secondary, fontSize = 10.sp)
        }
    }
}

private object V2Icons {
    fun forCategory(title: String): ImageVector = when (title) {
        "Deportes" -> Icons.Default.SportsSoccer
        "Películas" -> Icons.Default.Movie
        "Series" -> Icons.Default.Tv
        "Cristianos" -> Icons.Default.Church
        "Noticias" -> Icons.Default.Article
        "Niños y familia" -> Icons.Default.ChildCare
        "Música" -> Icons.Default.MusicNote
        "Documentales y cultura" -> Icons.Default.Description
        "Entretenimiento" -> Icons.Default.TheaterComedy
        "TV general" -> Icons.Default.LiveTv
        "Adultos 18+" -> Icons.Default.Lock
        else -> Icons.Default.Category
    }
}

@Composable
private fun V2RefreshSummary(ui: AppUiState) {
    ui.lastRefresh?.let { r ->
        Spacer(Modifier.height(6.dp))
        Text(
            if (r.added + r.removed + r.changed == 0) "✓ Lista al día · ${r.channelCount} canales"
            else "✓ +${r.added} · -${r.removed} · ${r.changed} modificados · ${r.channelCount} canales",
            color = V2Secondary,
            fontSize = 11.sp
        )
    }
}

private fun v2Filtered(ui: AppUiState, filter: String): List<Channel> = when {
    TextNormalizer.normalize(filter) == "favoritos" -> ui.channels.distinctBy { it.id }.filter { it.id in ui.favorites }
    filter.startsWith("País:") -> ChannelClassifier.channelsForCountry(ui.channels, filter.removePrefix("País:"))
    else -> ChannelClassifier.channelsForCategory(ui.channels, filter)
}

@Composable
private fun V2Channels(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean) {
    if (TextNormalizer.normalize(filter) == "paises") {
        V2Countries(ui, vm, phone)
        return
    }
    val list = remember(ui.channels, ui.favorites, filter) { v2Filtered(ui, filter) }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id, phone) {
        preview = null
        if (!phone) { delay(1200); preview = focused }
    }
    val title = filter.removePrefix("País:")

    if (phone) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            V2ListHeader(title, list.size)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { c -> V2ChannelCard(c, ui, vm, Modifier.fillMaxWidth().height(116.dp), true) { focused = c } }
            }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.weight(.82f)) {
                V2ListHeader(title, list.size)
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    preview?.let { ResilientPlayer(it, Modifier.fillMaxSize(), volume = 0f, controls = false) }
                    if (preview == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Tv, null, tint = V2Secondary, modifier = Modifier.size(42.dp))
                        Text("Mantén el foco para previsualizar", color = V2Secondary)
                    }
                }
                focused?.let { Spacer(Modifier.height(7.dp)); Text(cleanChannelName(it), fontWeight = FontWeight.Bold); Text(it.group, color = V2Secondary, fontSize = 11.sp) }
            }
            Spacer(Modifier.width(14.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { c -> V2ChannelCard(c, ui, vm, Modifier.fillMaxWidth().height(106.dp), false) { focused = c } }
            }
        }
    }
}

@Composable
private fun V2ListHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count canales", color = V2Secondary, fontSize = 11.sp)
    }
}

@Composable
private fun V2Countries(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val countries = remember(ui.channels) { ChannelClassifier.countries(ui.channels) }
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Países", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Un folder por país · sin canales repetidos dentro de cada país", color = V2Secondary, fontSize = 11.sp)
            }
            Text("${countries.size} países", color = V2Secondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (phone) 2 else 4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(countries, key = { it.name }) { country ->
                V2FocusCard({ vm.openCategory("País:${country.name}") }, Modifier.fillMaxWidth().height(if (phone) 96.dp else 104.dp)) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = if (TextNormalizer.normalize(country.name) == "honduras") V2Accent else V2Secondary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(country.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${country.count} canales", color = V2Secondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V2ChannelCard(c: Channel, ui: AppUiState, vm: MainViewModel, modifier: Modifier, phone: Boolean, onFocus: () -> Unit) {
    V2FocusCard({ vm.play(c) }, modifier, onFocus) {
        Column(Modifier.fillMaxSize().padding(if (phone) 8.dp else 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(if (phone) 42.dp else 48.dp), contentScale = ContentScale.Fit)
                else Box(Modifier.size(if (phone) 42.dp else 48.dp).background(V2Panel2, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text(cleanChannelName(c).take(2).uppercase(), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(7.dp))
                Text(cleanChannelName(c), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = if (phone) 12.sp else 14.sp)
                IconButton(onClick = { vm.toggleFavorite(c.id) }, modifier = Modifier.size(34.dp)) {
                    Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (c.id in ui.favorites) V2Live else V2Secondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Row {
                Text(ChannelClassifier.categoryFor(c), color = V2Secondary, fontSize = 9.sp, maxLines = 1, modifier = Modifier.weight(1f))
                qualityFromName(c.name)?.let { Text(it, color = V2Secondary, fontSize = 9.sp) }
            }
        }
    }
}

@Composable
private fun V2Search(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
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
            Text("Ignora mayúsculas, acentos y tolera pequeños errores. Prioriza lo más cercano a la fecha y hora actuales.", color = V2Secondary, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            V2QuickRows(ui, vm)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ui.searchResults, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                    V2FocusCard({ vm.play(hit.channel) }, Modifier.fillMaxWidth().height(if (phone) 80.dp else 84.dp)) {
                        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            hit.channel.logo?.let { AsyncImage(it, null, Modifier.size(if (phone) 42.dp else 52.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(hit.program?.title ?: cleanChannelName(hit.channel), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(cleanChannelName(hit.channel), color = V2Secondary, fontSize = 10.sp, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (hit.temporalBucket == TemporalBucket.LIVE_NOW) "● EN VIVO" else hit.reason.uppercase(), color = if (hit.temporalBucket == TemporalBucket.LIVE_NOW) V2Live else V2Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
private fun V2QuickRows(ui: AppUiState, vm: MainViewModel) {
    val recent = ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }.take(10)
    val fav = ui.channels.filter { it.id in ui.favorites }.take(10)
    if (recent.isNotEmpty()) {
        Text("Recientes", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(recent, key = { it.id }) { c -> V2Mini(c) { vm.play(c) } } }
        Spacer(Modifier.height(10.dp))
    }
    if (fav.isNotEmpty()) {
        Text("Favoritos", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(fav, key = { it.id }) { c -> V2Mini(c) { vm.play(c) } } }
    }
}

@Composable
private fun V2Mini(c: Channel, click: () -> Unit) {
    V2FocusCard(click, Modifier.width(160.dp).height(66.dp)) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            c.logo?.let { AsyncImage(it, null, Modifier.size(36.dp)) }
            Spacer(Modifier.width(6.dp)); Text(cleanChannelName(c), maxLines = 2, fontSize = 11.sp)
        }
    }
}

@Composable
private fun V2Player(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val c = ui.selectedChannel ?: return
    var error by remember(c.id) { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ResilientPlayer(c, Modifier.fillMaxSize(), 1f, true) { error = it }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.76f)).padding(if (phone) 7.dp else 9.dp)) {
            Text(cleanChannelName(c), fontWeight = FontWeight.Bold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1)
            Text(ChannelClassifier.categoryFor(c), color = V2Secondary, fontSize = 10.sp, maxLines = 1)
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
private fun V2Dual(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    if (phone && portrait) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            V2DualPane(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), true)
            Box(Modifier.fillMaxWidth().height(2.dp).background(V2Accent))
            V2DualPane(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), true)
        }
    } else {
        Row(Modifier.fillMaxSize().background(Color.Black)) {
            V2DualPane(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), phone)
            Box(Modifier.width(2.dp).fillMaxHeight().background(V2Accent))
            V2DualPane(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), phone)
        }
    }
}

@Composable
private fun V2DualPane(side: DualSide, channel: Channel?, ui: AppUiState, audio: Boolean, vm: MainViewModel, modifier: Modifier, compact: Boolean) {
    var q by remember(side) { mutableStateOf("") }
    var results by remember(side) { mutableStateOf<List<SearchHit>>(emptyList()) }
    LaunchedEffect(q, ui.channels, ui.programs) {
        delay(220)
        results = if (q.length >= 2) SearchEngine.search(q, ui.channels, ui.programs, limit = 25) else emptyList()
    }
    Column(modifier.fillMaxHeight().background(if (audio) Color(0xFF071C1A) else Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(if (compact) 3.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(q, { q = it }, Modifier.weight(1f), leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }, placeholder = { Text(if (side == DualSide.LEFT) "Buscar izquierda…" else "Buscar derecha…", fontSize = if (compact) 10.sp else 12.sp) }, singleLine = true)
            IconButton({ vm.setDualAudio(side) }, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) { Text(if (audio) "🔊" else "🔇", fontSize = 16.sp) }
            IconButton({ vm.fullScreenFromDual(side) }, enabled = channel != null, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) { Icon(Icons.Default.Fullscreen, "Agrandar") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (channel != null) {
                ResilientPlayer(channel, Modifier.fillMaxSize(), if (audio) 1f else 0f, false)
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(.65f)).padding(5.dp)) {
                    Text(cleanChannelName(channel), fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(if (audio) "AUDIO" else "MUDO", color = if (audio) V2Accent else V2Secondary, fontSize = 8.sp)
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Busca un canal", color = V2Secondary, fontSize = 11.sp) }
            if (q.length >= 2) {
                LazyColumn(Modifier.fillMaxSize().background(Color(0xEE101114)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                        V2FocusCard({ vm.setDualChannel(side, hit.channel); q = "" }, Modifier.fillMaxWidth().height(if (compact) 48.dp else 56.dp)) {
                            Row(Modifier.fillMaxSize().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(hit.program?.title ?: cleanChannelName(hit.channel), maxLines = 1, fontSize = if (compact) 9.sp else 11.sp)
                                    Text(cleanChannelName(hit.channel), color = V2Secondary, fontSize = if (compact) 7.sp else 9.sp, maxLines = 1)
                                }
                                if (hit.temporalBucket == TemporalBucket.LIVE_NOW) Text("●", color = V2Live)
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
private fun V2Playlists(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }
    val clear = { name = ""; url = ""; editId = null }

    if (phone) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            item { Text("Mis listas", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            items(ui.playlists, key = { it.id }) { p -> V2PlaylistCard(p, vm, { if (p.id != LocalStore.DEFAULT_ID) { name = p.name; url = p.url; editId = p.id } }, true) }
            item { V2PlaylistEditor(name, url, editId, ui, vm, { name = it }, { url = it }, clear) }
            item { V2Legal() }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.weight(1.35f)) {
                Text("Mis listas", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ui.playlists, key = { it.id }) { p -> V2PlaylistCard(p, vm, { if (p.id != LocalStore.DEFAULT_ID) { name = p.name; url = p.url; editId = p.id } }, false) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { V2PlaylistEditor(name, url, editId, ui, vm, { name = it }, { url = it }, clear) }
        }
    }
}

@Composable
private fun V2PlaylistCard(p: PlaylistConfig, vm: MainViewModel, edit: () -> Unit, phone: Boolean) {
    val isDefault = p.id == LocalStore.DEFAULT_ID
    V2FocusCard(edit, Modifier.fillMaxWidth().height(if (phone) 100.dp else 90.dp)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (p.active) "✓ " else "") + p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                if (isDefault) Text("Predeterminada · protegida", color = V2Accent, fontSize = 10.sp)
                Text(p.url, color = V2Secondary, fontSize = 9.sp, maxLines = 1)
                Text(if (p.lastUpdatedEpochMs > 0) "Actualizada ${v2Fmt(p.lastUpdatedEpochMs)}" else "Sin actualización", color = V2Secondary, fontSize = 9.sp)
            }
            if (isDefault) Icon(Icons.Default.Lock, "Protegida", tint = V2Accent)
            if (!p.active) TextButton({ vm.activatePlaylist(p.id) }) { Text("Usar") }
        }
    }
}

@Composable
private fun V2PlaylistEditor(name: String, url: String, editId: String?, ui: AppUiState, vm: MainViewModel, setName: (String) -> Unit, setUrl: (String) -> Unit, clear: () -> Unit) {
    Column {
        Text(if (editId == null) "Agregar lista" else "Editar lista", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(name, setName, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(url, setUrl, Modifier.fillMaxWidth(), label = { Text("URL M3U") }, singleLine = true)
        Spacer(Modifier.height(7.dp))
        Button({ if (editId == null) vm.addPlaylist(name, url) else vm.editPlaylist(editId, name, url); clear() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(" Guardar") }
        if (editId != null && editId != LocalStore.DEFAULT_ID) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton({ vm.deletePlaylist(editId); clear() }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Text(" Eliminar") }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton({ vm.refresh(false) }, Modifier.fillMaxWidth(), enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, null); Text(" Actualizar lista activa") }
        Spacer(Modifier.height(6.dp))
        Text("La lista predeterminada de TV Español+ está protegida y no se puede eliminar ni modificar.", color = V2Secondary, fontSize = 10.sp)
    }
}

@Composable
private fun V2Settings(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    if (phone) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Configuración", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            item { V2Setting(Icons.Default.Tv, "Mis listas", "Agregar, seleccionar y actualizar listas") { vm.go(Screen.PLAYLISTS) } }
            item { V2Setting(Icons.Default.Refresh, "Actualización", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) } }
            item { V2Setting(Icons.Default.Search, "Súper Búsqueda", "Acentos, fuzzy y prioridad por fecha/hora") { vm.go(Screen.SEARCH) } }
            item { V2Setting(Icons.Default.Fullscreen, "Vista doble", "Dos canales, dos buscadores, un audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) } }
            item { V2Setting(Icons.Default.Article, "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) } }
            item { V2StatusPanel(ui) }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Configuración", fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
                V2Setting(Icons.Default.Tv, "Mis listas", "Agregar, seleccionar y actualizar listas") { vm.go(Screen.PLAYLISTS) }
                V2Setting(Icons.Default.Refresh, "Actualización", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) }
                V2Setting(Icons.Default.Search, "Súper Búsqueda", "Acentos, fuzzy y prioridad por fecha/hora") { vm.go(Screen.SEARCH) }
                V2Setting(Icons.Default.Fullscreen, "Vista doble", "Dos canales, dos buscadores, un audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) }
                V2Setting(Icons.Default.Article, "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) }
            }
            Spacer(Modifier.width(22.dp))
            V2StatusPanel(ui, Modifier.weight(1f))
        }
    }
}

@Composable
private fun V2Setting(icon: ImageVector, title: String, subtitle: String, click: () -> Unit) {
    V2FocusCard(click, Modifier.fillMaxWidth().height(68.dp).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = V2Accent, modifier = Modifier.size(21.dp)); Spacer(Modifier.width(9.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = V2Secondary, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun V2StatusPanel(ui: AppUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(V2Panel, RoundedCornerShape(13.dp)).padding(15.dp)) {
        Text("Estado", fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp))
        V2Status("Lista activa", ui.activePlaylist?.name ?: "—")
        V2Status("Canales", ui.channels.size.toString())
        V2Status("Categorías", ChannelClassifier.categories.count { ChannelClassifier.categoryCount(ui.channels, it.title) > 0 }.toString())
        V2Status("Países", ChannelClassifier.countries(ui.channels).size.toString())
        V2Status("EPG", if (ui.programs.isNotEmpty()) "Disponible" else "Sin datos")
        V2Status("Favoritos", ui.favorites.size.toString())
        V2Status("Última actualización", ui.activePlaylist?.lastUpdatedEpochMs?.takeIf { it > 0 }?.let(::v2Fmt) ?: "—")
        Spacer(Modifier.height(14.dp)); V2Legal()
    }
}

@Composable
private fun V2Status(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label, color = V2Secondary, modifier = Modifier.weight(1f), fontSize = 11.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) }
}

@Composable
private fun V2About(phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 20.dp else 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        V2Brand(phone)
        Spacer(Modifier.height(12.dp))
        Text("Versión 1.2.0 BETA", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Android TV · Google TV · Android móvil · Kotlin · Media3", color = V2Secondary, fontSize = 11.sp)
        Text("M3U + EPG + Súper Búsqueda + Vista doble + categorías exclusivas + países por folder.", color = V2Accent, modifier = Modifier.padding(top = 7.dp), fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        V2Legal()
    }
}

@Composable
private fun V2Legal() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("© epalma", color = V2Secondary, fontSize = 13.sp)
        Text("+504 99461582 - Tegucigalpa - Honduras", color = V2Secondary, fontSize = 12.sp)
    }
}

@Composable
private fun Box.V2ErrorBar(message: String, close: () -> Unit) {}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.V2ErrorBar(message: String, close: () -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).padding(10.dp).background(Color(0xEE351717), RoundedCornerShape(9.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), fontSize = 11.sp); IconButton(close) { Icon(Icons.Default.Close, "Cerrar") }
    }
}

@Composable
private fun V2FocusCard(click: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val border by animateColorAsState(if (focused) V2Accent else Color.Transparent, label = "v2-focus")
    Card(
        modifier = modifier.scale(if (focused) 1.02f else 1f).border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(10.dp)).clickable(interactionSource = interaction, indication = null, onClick = click).focusable(interactionSource = interaction),
        colors = CardDefaults.cardColors(containerColor = V2Panel),
        shape = RoundedCornerShape(10.dp)
    ) { content() }
}

private fun cleanChannelName(channel: Channel): String = channel.name.replace(Regex("\\s*\\((?:[0-9]{3,4}p|HD|FHD|UHD|4K)\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()
private fun qualityFromName(name: String): String? = Regex("\\(([^()]*(?:[0-9]{3,4}p|HD|FHD|UHD|4K)[^()]*)\\)\\s*$", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)
private fun v2Fmt(epoch: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epoch))
