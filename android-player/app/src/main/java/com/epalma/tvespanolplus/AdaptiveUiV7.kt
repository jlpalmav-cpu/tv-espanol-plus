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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val V7BG = Color(0xFF060A11)
private val V7NAVY = Color(0xFF0A234B)
private val V7BLUE = Color(0xFF154A8A)
private val V7WHITE = Color(0xFFF5F7FA)
private val V7PANEL = Color(0xEC121A27)
private val V7PANEL2 = Color(0xF01A2940)
private val V7ACCENT = Color(0xFF3BA9FF)
private val V7SECOND = Color(0xFFB8C4D6)
private val V7LIVE = Color(0xFFE65058)

private data class MenuV7(val label: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun TVEspanolPlusRootV7(vm: MainViewModel, onExit: () -> Unit) {
    val screen by vm.screen.collectAsState()
    val ui by vm.ui.collectAsState()
    val filter by vm.filter.collectAsState()
    val prefs by vm.preferences.collectAsState()
    val gate by vm.parentalGate.collectAsState()
    val parentalUnlocked by vm.parentalUnlocked.collectAsState()
    val context = LocalContext.current
    val cfg = LocalConfiguration.current
    val phone = !remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val portrait = cfg.screenHeightDp >= cfg.screenWidthDp
    val density = LocalDensity.current
    var confirmExit by remember { mutableStateOf(false) }

    LaunchedEffect(ui.channels) {
        if (ui.channels.isNotEmpty()) CatalogCache.prewarm(ui.channels)
    }

    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * prefs.uiTextScale)) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = V7ACCENT,
                background = V7BG,
                surface = V7PANEL,
                onBackground = V7WHITE,
                onSurface = V7WHITE
            )
        ) {
            when (screen) {
                Screen.HOME -> V7Frame(phone, vm, ui.refreshing, { confirmExit = true }) { HomeV7(ui, vm, phone, portrait) { confirmExit = true } }
                Screen.CHANNELS -> V7Frame(phone, vm, ui.refreshing, { confirmExit = true }) { ChannelsV7(ui, filter, vm, phone) }
                Screen.SEARCH -> V7Frame(phone, vm, ui.refreshing, { confirmExit = true }) { UniversalSearchV7(ui, vm, phone) }
                Screen.DUAL -> DualViewV7(ui, vm, phone, portrait)
                Screen.SETTINGS -> V7Frame(phone, vm, ui.refreshing, { confirmExit = true }) { SettingsV7(ui, prefs, parentalUnlocked, vm, phone) }
                Screen.ABOUT -> V7Frame(phone, vm, ui.refreshing, { confirmExit = true }) { AboutV7(phone) }
                else -> TVEspanolPlusRootV6(vm, onExit)
            }
            if (screen in setOf(Screen.HOME, Screen.CHANNELS, Screen.SEARCH, Screen.DUAL, Screen.SETTINGS, Screen.ABOUT)) {
                gate?.let { ParentalGateV7(it, vm) }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Deseas salir de TV Español+?") },
            text = { Text("Se cerrará la aplicación y se liberarán los reproductores.") },
            confirmButton = { TextButton(onClick = onExit) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun OlimpiaMotaguaBackgroundV7() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF11151C),
                    Color(0xFFEEF2F7).copy(alpha = .12f),
                    V7BG,
                    Color(0xFF071936),
                    Color(0xFF0C2F67)
                )
            )
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x221F6CC5), Color.Transparent, Color(0xE8060A11))
                )
            )
        )
    }
}

@Composable
private fun V7Frame(phone: Boolean, vm: MainViewModel, refreshing: Boolean, onExit: () -> Unit, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Transparent) {
        Box(Modifier.fillMaxSize()) {
            OlimpiaMotaguaBackgroundV7()
            Column(Modifier.fillMaxSize()) {
                HeaderV7(phone, vm, refreshing, onExit)
                Box(Modifier.weight(1f)) { content() }
                if (phone) BottomV7(vm)
            }
        }
    }
}

@Composable
private fun HeaderV7(phone: Boolean, vm: MainViewModel, refreshing: Boolean, onExit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(if (phone) 60.dp else 78.dp).background(Color(0xEE07111F)).padding(horizontal = if (phone) 8.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.clickable { vm.go(Screen.HOME) }, verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 40.dp else 54.dp))
            Spacer(Modifier.width(7.dp))
            Column {
                Text("TV Español+", fontSize = if (phone) 18.sp else 26.sp, fontWeight = FontWeight.Black)
                if (!phone) Text("Honduras · entretenimiento en español", color = V7SECOND, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        if (!phone) {
            HeaderButtonV7(Icons.Default.Search, "Buscar") { vm.openSearch() }
            HeaderButtonV7(Icons.Default.Refresh, if (refreshing) "Actualizando" else "Actualizar", !refreshing) { vm.refresh(false) }
            HeaderButtonV7(Icons.Default.Settings, "Ajustes") { vm.go(Screen.SETTINGS) }
            HeaderButtonV7(Icons.Default.ExitToApp, "Salir") { onExit() }
        } else {
            IconButton({ vm.openSearch() }) { Icon(Icons.Default.Search, "Buscar") }
            IconButton({ vm.go(Screen.SETTINGS) }) { Icon(Icons.Default.Settings, "Ajustes") }
            IconButton(onExit) { Icon(Icons.Default.ExitToApp, "Salir") }
        }
    }
}

@Composable
private fun HeaderButtonV7(icon: ImageVector, text: String, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(action, Modifier.padding(start = 6.dp).height(46.dp), enabled = enabled) {
        Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(text)
    }
}

@Composable
private fun BottomV7(vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xF207111F)), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BottomButtonV7(Icons.Default.Home, "Inicio") { vm.go(Screen.HOME) }
        BottomButtonV7(Icons.Default.Search, "Buscar") { vm.openSearch() }
        BottomButtonV7(Icons.Default.Favorite, "Favoritos") { vm.openCategory("Favoritos") }
        BottomButtonV7(Icons.Default.Settings, "Ajustes") { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun BottomButtonV7(icon: ImageVector, label: String, action: () -> Unit) {
    TextButton(action) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, Modifier.size(21.dp), tint = V7SECOND)
            Text(label, color = V7SECOND, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeV7(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean, onExit: () -> Unit) {
    val recent = remember(ui.recentIds, ui.channels) {
        ui.recentIds.mapNotNull { id -> ui.channels.firstOrNull { it.id == id } }.distinctBy { it.id }.take(10)
    }
    val hero = recent.firstOrNull() ?: ui.channels.firstOrNull()
    val menu = listOf(
        MenuV7("TV en vivo", Icons.Default.LiveTv) { vm.openCategory("TV general") },
        MenuV7("Películas", Icons.Default.Movie) { vm.openCategory("Películas") },
        MenuV7("Series", Icons.Default.Tv) { vm.openCategory("Series") },
        MenuV7("Deportes", Icons.Default.SportsSoccer) { vm.openCategory("Deportes") },
        MenuV7("Noticias", Icons.Default.Article) { vm.openCategory("Noticias") },
        MenuV7("Familia", Icons.Default.ChildCare) { vm.openCategory("Niños y familia") },
        MenuV7("Países", Icons.Default.Public) { vm.openCategory("Países") },
        MenuV7("Favoritos", Icons.Default.Favorite) { vm.openCategory("Favoritos") },
        MenuV7("Buscar", Icons.Default.Search) { vm.openSearch() },
        MenuV7("Dual View", Icons.Default.Fullscreen) {
            val base = recent.firstOrNull() ?: ui.selectedChannel ?: ui.channels.firstOrNull()
            if (base != null) vm.startDual(base)
        },
        MenuV7("Configuración", Icons.Default.Settings) { vm.go(Screen.SETTINGS) },
        MenuV7("Salir", Icons.Default.ExitToApp, onExit)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (phone && portrait) 2 else 4),
        modifier = Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (ui.channels.isEmpty()) {
                EmptyV7(ui, vm, phone)
            } else {
                HeroV7(hero, ui, vm, phone, portrait)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column { Text("Explorar", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Accesos grandes y navegación rápida", color = V7SECOND, fontSize = 10.sp) }
        }
        items(menu, key = { it.label }) { entry -> BigMenuV7(entry, phone) }
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Recientes", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent, key = { it.id }) { channel -> MiniChannelV7(channel, phone) { vm.play(channel) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroV7(channel: Channel?, ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val now = System.currentTimeMillis()
    val program = channel?.tvgId?.let { ui.programs[it]?.firstOrNull { p -> p.isLive(now) } }
    Card(
        Modifier.fillMaxWidth().height(if (phone && portrait) 170.dp else 210.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD80B1422)), shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            channel?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .11f) }
            Column(Modifier.align(Alignment.BottomStart).padding(if (phone) 13.dp else 18.dp)) {
                Text(program?.title ?: channel?.name.orEmpty().ifBlank { "TV Español+" }, fontSize = if (phone) 22.sp else 29.sp, fontWeight = FontWeight.Black, maxLines = 2)
                channel?.let { Text("${ChannelClassifier.categoryFor(it)} · ${ChannelClassifier.countryName(it)}", color = V7SECOND, fontSize = 10.sp) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ channel?.let(vm::play) }, enabled = channel != null) { Icon(Icons.Default.PlayArrow, null); Text(" Ver") }
                    OutlinedButton({ channel?.let(vm::startDual) }, enabled = channel != null) { Icon(Icons.Default.Fullscreen, null); Text(" Dual") }
                }
            }
        }
    }
}

@Composable
private fun EmptyV7(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    Column(Modifier.fillMaxWidth().height(if (phone) 170.dp else 210.dp).background(V7PANEL, RoundedCornerShape(16.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(if (ui.refreshing) Icons.Default.Refresh else Icons.Default.TvOff, null, tint = V7ACCENT, modifier = Modifier.size(42.dp))
        Text(if (ui.refreshing) "Preparando canales…" else "Sin catálogo disponible", fontWeight = FontWeight.Bold)
        Text(ui.statusMessage, color = V7SECOND, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp)); Button({ vm.refresh(false) }, enabled = !ui.refreshing) { Text("Reintentar") }
    }
}

@Composable
private fun BigMenuV7(entry: MenuV7, phone: Boolean) {
    FocusCardV7(entry.action, Modifier.fillMaxWidth().height(if (phone) 96.dp else 112.dp)) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(entry.icon, null, tint = V7WHITE, modifier = Modifier.size(if (phone) 31.dp else 38.dp))
            Spacer(Modifier.height(7.dp)); Text(entry.label, fontWeight = FontWeight.Bold, fontSize = if (phone) 11.sp else 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MiniChannelV7(channel: Channel, phone: Boolean, action: () -> Unit) {
    FocusCardV7(action, Modifier.width(if (phone) 155.dp else 190.dp).height(if (phone) 68.dp else 78.dp)) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            channel.logo?.let { AsyncImage(it, null, Modifier.size(40.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(7.dp)) }
            Text(channel.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChannelsV7(ui: AppUiState, route: String, vm: MainViewModel, phone: Boolean) {
    val index = remember(ui.channels) { CatalogCache.get(ui.channels) }
    when {
        TextNormalizer.normalize(route) == "paises" -> FolderGridV7("Países", "Honduras aparece primero", index.countries(), phone) { vm.openCategory("País:$it") }
        route.startsWith("País:") -> {
            val country = route.removePrefix("País:")
            val groups = index.categoriesForCountry(country)
            if (groups.size <= 1) ChannelBrowserV7(country, index.country(country), ui, vm, phone)
            else FolderGridV7(country, "Elige el tipo de contenido", groups, phone) { vm.openCategory("CountryCat:$country|$it") }
        }
        route.startsWith("CountryCat:") -> {
            val payload = route.removePrefix("CountryCat:")
            val country = payload.substringBefore('|')
            val category = payload.substringAfter('|', "TV general")
            val list = index.country(country).filter { ChannelClassifier.categoryFor(it) == category }
            ChannelBrowserV7("$country · $category", list, ui, vm, phone)
        }
        route.startsWith("Subcat:") -> {
            val payload = route.removePrefix("Subcat:")
            val category = payload.substringBefore('|')
            val sub = payload.substringAfter('|')
            ChannelBrowserV7("$category · $sub", index.subcategory(category, sub), ui, vm, phone)
        }
        TextNormalizer.normalize(route) == "favoritos" -> ChannelBrowserV7("Favoritos", ui.favorites.mapNotNull(index::channel), ui, vm, phone)
        TextNormalizer.normalize(route) == "recientes" -> ChannelBrowserV7("Recientes", ui.recentIds.mapNotNull(index::channel), ui, vm, phone)
        else -> {
            val folders = index.subcategories(route)
            if (folders.isNotEmpty() && index.category(route).size >= ChannelClassifier.SUBCATEGORY_THRESHOLD) {
                FolderGridV7(route, smartSubtitleV7(route), folders, phone) { vm.openCategory("Subcat:$route|$it") }
            } else ChannelBrowserV7(route, index.category(route), ui, vm, phone)
        }
    }
}

private fun smartSubtitleV7(category: String): String = when (category) {
    "Deportes" -> "Elige deporte; después encuentra el canal rápidamente"
    "Películas" -> "Elige género"
    "Series" -> "Elige género"
    "Noticias" -> "Elige país"
    else -> "Submenús sin repetir contenido"
}

@Composable
private fun FolderGridV7(title: String, subtitle: String, groups: List<CatalogFolder>, phone: Boolean, open: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V7SECOND, fontSize = 10.sp) }
            Text("${groups.sumOf { it.count }}", color = V7SECOND, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(groups, key = { it.name }) { folder ->
                FocusCardV7({ open(folder.name) }, Modifier.fillMaxWidth().height(if (phone) 96.dp else 108.dp)) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Icon(if (TextNormalizer.normalize(folder.name) == "honduras") Icons.Default.Public else Icons.Default.Folder, null, tint = V7ACCENT, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(5.dp)); Text(folder.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${folder.count} canales", color = V7SECOND, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBrowserV7(title: String, raw: List<Channel>, ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val list = remember(raw) { raw.distinctBy { it.id } }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember(list) { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id, phone) {
        preview = null
        if (!phone && focused != null) { delay(1200); preview = focused }
    }

    if (phone) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            BrowserTitleV7(title, list.size)
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { channel -> ChannelCardV7(channel, ui, vm, true) { focused = channel } }
            }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.weight(.82f)) {
                BrowserTitleV7(title, list.size); Spacer(Modifier.height(8.dp))
                PreviewV7(focused, preview, ui, vm, Modifier.fillMaxWidth().weight(1f))
            }
            Spacer(Modifier.width(14.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { channel -> ChannelCardV7(channel, ui, vm, false) { focused = channel } }
            }
        }
    }
}

@Composable
private fun BrowserTitleV7(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count canales", color = V7SECOND, fontSize = 10.sp)
    }
}

@Composable
private fun PreviewV7(selected: Channel?, preview: Channel?, ui: AppUiState, vm: MainViewModel, modifier: Modifier) {
    Column(modifier.background(Color(0xE609121F), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
            if (preview != null) ResilientPlayer(preview, Modifier.fillMaxSize(), volume = 0f, controls = false)
            else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LiveTv, null, tint = V7SECOND, modifier = Modifier.size(44.dp)); Text("Información inmediata · preview tras 1.2 s", color = V7SECOND, fontSize = 9.sp)
            }
        }
        selected?.let { channel ->
            Spacer(Modifier.height(7.dp)); Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Text("${ChannelClassifier.categoryFor(channel)} · ${ChannelClassifier.countryName(channel)}", color = V7SECOND, fontSize = 9.sp)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button({ vm.play(channel) }, Modifier.weight(1f)) { Text("Ver") }
                OutlinedButton({ vm.startDual(channel) }, Modifier.weight(1f)) { Text("Dual") }
                OutlinedButton({ vm.toggleFavorite(channel.id) }, Modifier.weight(1f)) { Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) }
            }
        }
    }
}

@Composable
private fun ChannelCardV7(channel: Channel, ui: AppUiState, vm: MainViewModel, phone: Boolean, onFocus: () -> Unit) {
    FocusCardV7({ vm.play(channel) }, Modifier.fillMaxWidth().height(if (phone) 112.dp else 108.dp), onFocus) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (channel.logo != null) AsyncImage(channel.logo, null, Modifier.size(if (phone) 42.dp else 48.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Default.Tv, null, tint = V7SECOND, modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(7.dp)); Text(channel.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = if (phone) 11.sp else 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                IconButton({ vm.toggleFavorite(channel.id) }, Modifier.size(30.dp)) { Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (channel.id in ui.favorites) V7LIVE else V7SECOND, modifier = Modifier.size(17.dp)) }
            }
            Text(ChannelClassifier.countryName(channel), color = V7SECOND, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun UniversalSearchV7(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val index = remember(ui.channels) { CatalogCache.get(ui.channels) }
    val query = ui.searchQuery
    val fast = remember(query, index) { index.searchChannels(query, 60) }
    val directRoute = remember(query) { routeForQueryV7(query) }
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Text("Búsqueda Universal", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Busca escribiendo o entra directamente por categoría", color = V7SECOND, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            query,
            vm::search,
            Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotBlank()) IconButton({ vm.search("") }) { Icon(Icons.Default.Close, "Limpiar") } },
            placeholder = { Text("fútbol Honduras, acción, Barcelona, Fórmula 1…") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("Todos", "TV en vivo", "Deportes", "Películas", "Series", "Noticias", "Países")) { label ->
                OutlinedButton({
                    when (label) {
                        "Todos" -> vm.search("")
                        "TV en vivo" -> vm.openCategory("TV general")
                        "Países" -> vm.openCategory("Países")
                        else -> vm.openCategory(label)
                    }
                }) { Text(label, fontSize = 10.sp) }
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("Fútbol", "Acción", "Comedia", "Infantil", "Honduras", "Favoritos", "Recientes")) { label ->
                TextButton({
                    when (label) {
                        "Fútbol" -> vm.openCategory("Subcat:Deportes|Fútbol")
                        "Acción" -> vm.openCategory("Subcat:Películas|Acción")
                        "Comedia" -> vm.openCategory("Subcat:Películas|Comedia")
                        "Infantil" -> vm.openCategory("Niños y familia")
                        "Honduras" -> vm.openCategory("País:Honduras")
                        "Favoritos" -> vm.openCategory("Favoritos")
                        "Recientes" -> vm.openCategory("Recientes")
                    }
                }) { Text(label) }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (query.length < 2) {
            Text("Puedes elegir una categoría sin escribir nada.", color = V7SECOND, fontSize = 10.sp)
        } else {
            directRoute?.let { route ->
                FocusCardV7({ vm.openCategory(route.second) }, Modifier.fillMaxWidth().height(64.dp)) {
                    Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (route.second.startsWith("Subcat:Deportes") || route.second == "Deportes") Icons.Default.SportsSoccer else Icons.Default.Folder, null, tint = V7ACCENT)
                        Spacer(Modifier.width(8.dp)); Column { Text(route.first, fontWeight = FontWeight.Bold); Text("Abrir directamente", color = V7SECOND, fontSize = 9.sp) }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
            Text("Resultados inmediatos · ${fast.size}", color = V7SECOND, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(fast, key = { it.id }) { channel -> ChannelCardV7(channel, ui, vm, phone, {}) }
            }
        }
    }
}

private fun routeForQueryV7(query: String): Pair<String, String>? {
    val q = TextNormalizer.normalize(query)
    return when {
        q == "deportes" || q == "deporte" || q == "sport" -> "Deportes" to "Deportes"
        q.contains("futbol") || q == "soccer" -> "Deportes > Fútbol" to "Subcat:Deportes|Fútbol"
        q == "peliculas" || q == "pelicula" || q == "cine" -> "Películas" to "Películas"
        q.contains("accion") -> "Películas > Acción" to "Subcat:Películas|Acción"
        q.contains("comedia") -> "Películas > Comedia" to "Subcat:Películas|Comedia"
        q == "series" || q == "serie" -> "Series" to "Series"
        q.contains("noticias") || q == "news" -> "Noticias" to "Noticias"
        q == "honduras" -> "País > Honduras" to "País:Honduras"
        else -> null
    }
}

@Composable
private fun DualViewV7(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    var selector by remember { mutableStateOf<DualSide?>(if (ui.dualRight == null) DualSide.RIGHT else null) }
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            if (phone && portrait) {
                Column(Modifier.fillMaxSize()) {
                    DualPaneV7(DualSide.LEFT, ui.dualLeft, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), true) { selector = DualSide.LEFT }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(V7ACCENT))
                    DualPaneV7(DualSide.RIGHT, ui.dualRight, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), true) { selector = DualSide.RIGHT }
                    DualControlsV7(ui, vm) { selector = null }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        DualPaneV7(DualSide.LEFT, ui.dualLeft, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), phone) { selector = DualSide.LEFT }
                        Box(Modifier.width(2.dp).fillMaxHeight().background(V7ACCENT))
                        DualPaneV7(DualSide.RIGHT, ui.dualRight, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), phone) { selector = DualSide.RIGHT }
                    }
                    DualControlsV7(ui, vm) { selector = null }
                }
            }
            selector?.let { side -> ChannelSelectorV7(side, ui, vm, phone, Modifier.align(Alignment.CenterEnd)) { selector = null } }
        }
    }
}

@Composable
private fun DualPaneV7(side: DualSide, channel: Channel?, audio: Boolean, vm: MainViewModel, modifier: Modifier, compact: Boolean, choose: () -> Unit) {
    Box(modifier.fillMaxHeight().background(Color.Black)) {
        if (channel != null) {
            ResilientPlayer(channel, Modifier.fillMaxSize(), volume = if (audio) 1f else 0f, controls = false)
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.72f)).padding(7.dp)) {
                Text(channel.name, fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 12.sp, maxLines = 1)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(choose, Modifier.weight(1f).height(40.dp)) { Text("Cambiar canal", fontSize = 9.sp) }
                    OutlinedButton({ vm.setDualAudio(side) }, Modifier.weight(1f).height(40.dp)) { Text(if (audio) "🔊 Audio" else "🔇 Activar", fontSize = 9.sp) }
                    OutlinedButton({ vm.fullScreenFromDual(side) }, Modifier.weight(1f).height(40.dp)) { Icon(Icons.Default.Fullscreen, null, Modifier.size(16.dp)) }
                }
            }
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Tv, null, tint = V7SECOND, modifier = Modifier.size(44.dp)); Spacer(Modifier.height(7.dp))
                Button(choose) { Text("+ Elegir canal") }
            }
        }
        if (audio) Text("AUDIO", Modifier.align(Alignment.TopStart).padding(6.dp).background(V7BLUE, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DualControlsV7(ui: AppUiState, vm: MainViewModel, closeSelector: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xF207111F)).padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        TextButton({ vm.swapDual() }) { Icon(Icons.Default.SwapHoriz, null); Text(" Intercambiar") }
        TextButton({ vm.setDualAudio(if (ui.dualAudioSide == DualSide.LEFT) DualSide.RIGHT else DualSide.LEFT) }) { Icon(Icons.Default.Language, null); Text(" Cambiar audio") }
        TextButton({ closeSelector(); vm.go(Screen.HOME) }) { Icon(Icons.Default.Close, null); Text(" Salir Dual") }
    }
}

@Composable
private fun ChannelSelectorV7(side: DualSide, ui: AppUiState, vm: MainViewModel, phone: Boolean, modifier: Modifier, dismiss: () -> Unit) {
    val index = remember(ui.channels) { CatalogCache.get(ui.channels) }
    var query by remember(side) { mutableStateOf("") }
    var mode by remember(side) { mutableStateOf("Recientes") }
    val list = remember(query, mode, ui.channels, ui.favorites, ui.recentIds) {
        when {
            query.length >= 2 -> index.searchChannels(query, 50)
            mode == "Recientes" -> ui.recentIds.mapNotNull(index::channel)
            mode == "Favoritos" -> ui.favorites.mapNotNull(index::channel)
            mode.startsWith("Subcat:") -> {
                val p = mode.removePrefix("Subcat:"); index.subcategory(p.substringBefore('|'), p.substringAfter('|'))
            }
            mode.startsWith("País:") -> index.country(mode.removePrefix("País:"))
            else -> index.category(mode)
        }.distinctBy { it.id }
    }

    Surface(
        modifier = modifier.fillMaxHeight().fillMaxWidth(if (phone) 1f else .58f).border(2.dp, V7ACCENT, RoundedCornerShape(14.dp)),
        color = Color(0xFA08111D), shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Elegir canal ${if (side == DualSide.LEFT) "1" else "2"}", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("El otro canal continúa reproduciéndose", color = V7SECOND, fontSize = 9.sp) }
                IconButton(dismiss) { Icon(Icons.Default.Close, "Cerrar selector") }
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar canal…") }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(listOf("Recientes", "Favoritos", "Deportes", "Noticias", "Películas", "Series", "TV general", "Países")) { option ->
                    if (option == "Países") OutlinedButton({ mode = "País:Honduras"; query = "" }) { Text(option, fontSize = 9.sp) }
                    else if (mode == option) Button({ mode = option; query = "" }) { Text(option, fontSize = 9.sp) }
                    else OutlinedButton({ mode = option; query = "" }) { Text(option, fontSize = 9.sp) }
                }
            }
            if (mode == "Deportes" && query.isBlank()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(index.subcategories("Deportes")) { folder -> TextButton({ mode = "Subcat:Deportes|${folder.name}" }) { Text(folder.name, fontSize = 9.sp) } }
                }
            }
            if (mode == "Películas" && query.isBlank()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(index.subcategories("Películas")) { folder -> TextButton({ mode = "Subcat:Películas|${folder.name}" }) { Text(folder.name, fontSize = 9.sp) } }
                }
            }
            if (mode.startsWith("País:") && query.isBlank()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(index.countries().take(20)) { folder -> TextButton({ mode = "País:${folder.name}" }) { Text(folder.name, fontSize = 9.sp) } }
                }
            }
            Spacer(Modifier.height(5.dp)); Text("${list.size} canales", color = V7SECOND, fontSize = 9.sp)
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(list, key = { it.id }) { channel ->
                    FocusCardV7({ vm.setDualChannel(side, channel); dismiss() }, Modifier.fillMaxWidth().height(58.dp)) {
                        Row(Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            channel.logo?.let { AsyncImage(it, null, Modifier.size(40.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(7.dp)) }
                            Column(Modifier.weight(1f)) { Text(channel.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 10.sp); Text("${ChannelClassifier.categoryFor(channel)} · ${ChannelClassifier.countryName(channel)}", color = V7SECOND, fontSize = 8.sp, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsV7(ui: AppUiState, prefs: UserPreferences, parentalUnlocked: Boolean, vm: MainViewModel, phone: Boolean) {
    var showPin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(if (phone) 10.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Configuración", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Preferencias rápidas y persistentes", color = V7SECOND, fontSize = 10.sp) }
        item { SettingCardV7(Icons.Default.Tv, "Mis listas", "La lista Usuario duplicada de Xtream se limpia automáticamente") { vm.go(Screen.PLAYLISTS) } }
        item { SettingCardV7(Icons.Default.Refresh, "Actualizar ahora", "Actualiza en segundo plano y conserva la última lista válida") { vm.refresh(false) } }
        item { SettingCardV7(Icons.Default.Search, "Búsqueda Universal", "Categorías, géneros, país y búsqueda tolerante a errores") { vm.openSearch() } }
        item { SettingCardV7(Icons.Default.Fullscreen, "Dual View", "Selector universal de canal y un solo audio activo") { (ui.selectedChannel ?: ui.channels.firstOrNull())?.let(vm::startDual) } }
        item { SectionTitleV7("Audio y subtítulos") }
        item { ChoiceV7("Audio preferido", listOf("Español Latino" to "es-419", "Español" to "es", "Inglés" to "en", "Portugués" to "pt"), prefs.preferredAudioLanguage, vm::setPreferredAudioLanguage) }
        item { ChoiceV7("Subtítulos", listOf("Español" to "es", "Inglés" to "en", "Portugués" to "pt", "Desactivados" to "off"), prefs.preferredSubtitleLanguage, vm::setPreferredSubtitleLanguage) }
        item { ChoiceV7("Tamaño de subtítulos", listOf("Pequeño" to "16", "Normal" to "20", "Grande" to "24", "Extra grande" to "28"), prefs.subtitleTextSizeSp.toInt().toString()) { vm.setSubtitleTextSize(it.toFloat()) } }
        item { SectionTitleV7("Pantalla") }
        item { ChoiceV7("Tamaño del texto", listOf("Compacto" to "0.9", "Normal" to "1.0", "Grande" to "1.15", "Extra grande" to "1.3"), nearestScaleV7(prefs.uiTextScale)) { vm.setUiTextScale(it.toFloat()) } }
        item { SectionTitleV7("Control parental") }
        item {
            Column(Modifier.fillMaxWidth().background(V7PANEL, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = V7ACCENT); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text("Protección con PIN", fontWeight = FontWeight.Bold); Text(if (parentalUnlocked) "Desbloqueado en esta sesión" else "Adultos 18+ y canales bloqueados requieren PIN", color = V7SECOND, fontSize = 9.sp) }
                    Switch(prefs.parentalEnabled, { vm.setParentalEnabled(it) })
                }
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton({ showPin = true }, Modifier.weight(1f)) { Icon(Icons.Default.Lock, null); Text(if (prefs.parentalPinHash.isBlank()) " Crear PIN" else " Cambiar PIN") }
                    OutlinedButton({ vm.requestParentalUnlock() }, Modifier.weight(1f), enabled = prefs.parentalEnabled && !parentalUnlocked) { Text("Desbloquear") }
                }
            }
        }
        item { SettingCardV7(Icons.Default.Description, "Acerca de", "TV Español+ Player v1.6.0") { vm.go(Screen.ABOUT) } }
    }

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false; pin = "" },
            title = { Text("PIN parental") },
            text = { OutlinedTextField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) pin = it }, label = { Text("4 a 6 dígitos") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true) },
            confirmButton = { TextButton({ if (vm.setParentalPin(pin)) { showPin = false; pin = "" } }) { Text("Guardar") } },
            dismissButton = { TextButton({ showPin = false; pin = "" }) { Text("Cancelar") } }
        )
    }
}

@Composable private fun SectionTitleV7(title: String) { Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }

@Composable
private fun SettingCardV7(icon: ImageVector, title: String, subtitle: String, action: () -> Unit) {
    FocusCardV7(action, Modifier.fillMaxWidth().height(76.dp)) {
        Row(Modifier.fillMaxSize().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = V7ACCENT, modifier = Modifier.size(27.dp)); Spacer(Modifier.width(9.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = V7SECOND, fontSize = 9.sp, maxLines = 2) }
        }
    }
}

@Composable
private fun ChoiceV7(title: String, choices: List<Pair<String, String>>, selected: String, change: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(V7PANEL, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(choices, key = { it.second }) { (label, value) ->
                if (selected == value) Button({ change(value) }) { Text(label, fontSize = 9.sp) }
                else OutlinedButton({ change(value) }) { Text(label, fontSize = 9.sp) }
            }
        }
    }
}

@Composable
private fun AboutV7(phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 88.dp else 120.dp))
        Spacer(Modifier.height(10.dp)); Text("TV Español+ Player", fontSize = if (phone) 25.sp else 34.sp, fontWeight = FontWeight.Black)
        Text("Versión 1.6.0", color = V7ACCENT, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp)); Text("Android TV · Google TV · Android móvil", color = V7SECOND)
        Text("Búsqueda Universal · Dual View · catálogo indexado · control parental", color = V7SECOND, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp)); Text("Honduras", color = V7WHITE, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ParentalGateV7(gate: ParentalGate, vm: MainViewModel) {
    var pin by remember(gate) { mutableStateOf("") }
    var invalid by remember(gate) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = vm::dismissParentalGate,
        title = { Text(gate.title) },
        text = {
            Column {
                Text(gate.message); Spacer(Modifier.height(8.dp))
                OutlinedTextField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; invalid = false } }, label = { Text("PIN parental") }, isError = invalid, supportingText = { if (invalid) Text("PIN incorrecto") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
            }
        },
        confirmButton = { TextButton({ if (!vm.submitParentalPin(pin)) invalid = true }) { Text("Desbloquear") } },
        dismissButton = { TextButton(vm::dismissParentalGate) { Text("Cancelar") } }
    )
}

@Composable
private fun FocusCardV7(onClick: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val border by animateColorAsState(if (focused) V7ACCENT else Color.Transparent, label = "focus-v7")
    Card(
        modifier = modifier.scale(if (focused) 1.025f else 1f).border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(12.dp)).clickable(interactionSource = interaction, indication = null, onClick = onClick).focusable(interactionSource = interaction),
        colors = CardDefaults.cardColors(containerColor = if (focused) V7PANEL2 else V7PANEL), shape = RoundedCornerShape(12.dp)
    ) { content() }
}

private fun nearestScaleV7(value: Float): String {
    val choices = listOf(0.9f, 1.0f, 1.15f, 1.3f)
    return choices.minByOrNull { kotlin.math.abs(it - value) }?.toString() ?: "1.0"
}
