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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
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
import kotlin.math.abs

private val V6BG = Color(0xFF070B13)
private val V6PANEL = Color(0xE6172233)
private val V6PANEL2 = Color(0xE6243450)
private val V6ACCENT = Color(0xFF27A9FF)
private val V6TEXT = Color(0xFFF6F8FC)
private val V6SECOND = Color(0xFFB5C1D4)
private val V6LIVE = Color(0xFFE34B55)

private data class MenuEntryV6(val label: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun TVEspanolPlusRootV6(vm: MainViewModel, onExit: () -> Unit) {
    val screen by vm.screen.collectAsState()
    val ui by vm.ui.collectAsState()
    val filter by vm.filter.collectAsState()
    val prefs by vm.preferences.collectAsState()
    val gate by vm.parentalGate.collectAsState()
    val parentalUnlocked by vm.parentalUnlocked.collectAsState()
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val phone = !remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val portrait = config.screenHeightDp >= config.screenWidthDp
    val systemDensity = LocalDensity.current
    var confirmExit by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * prefs.uiTextScale)) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = V6ACCENT,
                background = V6BG,
                surface = V6PANEL,
                onSurface = V6TEXT,
                onBackground = V6TEXT
            )
        ) {
            Box(Modifier.fillMaxSize().background(V6BG)) {
                SportsFusionBackgroundV6()
                when (screen) {
                    Screen.HOME -> HomeScreenV6(ui, vm, phone, portrait) { confirmExit = true }
                    Screen.CHANNELS -> ChannelsScreenV6(ui, filter, vm, phone) { confirmExit = true }
                    Screen.PLAYER -> PlayerScreenV6(ui, prefs, vm, phone, portrait)
                    Screen.SETTINGS -> SettingsScreenV6(ui, prefs, parentalUnlocked, vm, phone) { confirmExit = true }
                    Screen.ABOUT -> AboutScreenV6(phone)
                    else -> TVEspanolPlusRootV5(vm, onExit)
                }
                if (screen in setOf(Screen.HOME, Screen.CHANNELS, Screen.PLAYER, Screen.SETTINGS, Screen.ABOUT)) {
                    ui.error?.let { ErrorBannerV6(it, vm::clearError, Modifier.align(Alignment.BottomCenter)) }
                }
                gate?.let { ParentalPinDialogV6(it, vm) }
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
private fun SportsFusionBackgroundV6() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF250A22),
                    Color(0xFF0A1C42),
                    Color(0xFF070B13),
                    Color(0xFF111827),
                    Color(0xFF342E1B)
                )
            )
        )
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x2200A3E0), Color.Transparent, Color(0xDD070B13)))
            )
        )
    }
}

@Composable
private fun AppShellV6(phone: Boolean, vm: MainViewModel, refreshing: Boolean, onExit: () -> Unit, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Transparent) {
        Column(Modifier.fillMaxSize()) {
            HeaderV6(phone, vm, refreshing, onExit)
            Box(Modifier.weight(1f)) { content() }
            if (phone) BottomNavV6(vm)
        }
    }
}

@Composable
private fun HeaderV6(phone: Boolean, vm: MainViewModel, refreshing: Boolean, onExit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(if (phone) 62.dp else 82.dp).background(Color(0xE808111F)).padding(horizontal = if (phone) 8.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.clickable { vm.go(Screen.HOME) }, verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 42.dp else 58.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("TV Español+", fontWeight = FontWeight.Black, fontSize = if (phone) 18.sp else 27.sp, maxLines = 1)
                if (!phone) Text("Honduras · tu mundo en español", color = V6SECOND, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        if (!phone) {
            HeaderActionV6(Icons.Default.Search, "Buscar") { vm.openSearch() }
            HeaderActionV6(Icons.Default.Refresh, if (refreshing) "Actualizando" else "Actualizar", !refreshing) { vm.refresh(false) }
            HeaderActionV6(Icons.Default.Settings, "Ajustes") { vm.go(Screen.SETTINGS) }
            HeaderActionV6(Icons.Default.ExitToApp, "Salir") { onExit() }
        } else {
            IconButton({ vm.openSearch() }) { Icon(Icons.Default.Search, "Buscar") }
            IconButton({ vm.go(Screen.SETTINGS) }) { Icon(Icons.Default.Settings, "Ajustes") }
        }
    }
}

@Composable
private fun HeaderActionV6(icon: ImageVector, label: String, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(action, Modifier.padding(start = 7.dp).height(48.dp), enabled = enabled) {
        Icon(icon, null, Modifier.size(21.dp)); Spacer(Modifier.width(6.dp)); Text(label)
    }
}

@Composable
private fun BottomNavV6(vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xEE08111F)), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BottomActionV6(Icons.Default.Home, "Inicio") { vm.go(Screen.HOME) }
        BottomActionV6(Icons.Default.Search, "Buscar") { vm.openSearch() }
        BottomActionV6(Icons.Default.Favorite, "Favoritos") { vm.openCategory("Favoritos") }
        BottomActionV6(Icons.Default.Settings, "Ajustes") { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun BottomActionV6(icon: ImageVector, label: String, action: () -> Unit) {
    TextButton(action) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, Modifier.size(21.dp), tint = V6SECOND)
            Text(label, color = V6SECOND, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeScreenV6(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean, onExit: () -> Unit) {
    AppShellV6(phone, vm, ui.refreshing, onExit) {
        val recent = remember(ui.recentIds, ui.channels) {
            ui.recentIds.mapNotNull { id -> ui.channels.firstOrNull { it.id == id } }.distinctBy { it.id }.take(10)
        }
        val hero = recent.firstOrNull() ?: ui.channels.firstOrNull()
        val menu = listOf(
            MenuEntryV6("TV en vivo", Icons.Default.LiveTv) { vm.openCategory("TV general") },
            MenuEntryV6("Películas", Icons.Default.Movie) { vm.openCategory("Películas") },
            MenuEntryV6("Series", Icons.Default.Tv) { vm.openCategory("Series") },
            MenuEntryV6("Deportes", Icons.Default.SportsSoccer) { vm.openCategory("Deportes") },
            MenuEntryV6("Niños", Icons.Default.ChildCare) { vm.openCategory("Niños y familia") },
            MenuEntryV6("Noticias", Icons.Default.Article) { vm.openCategory("Noticias") },
            MenuEntryV6("Música", Icons.Default.MusicNote) { vm.openCategory("Música") },
            MenuEntryV6("Países", Icons.Default.Public) { vm.openCategory("Países") },
            MenuEntryV6("Favoritos", Icons.Default.Favorite) { vm.openCategory("Favoritos") },
            MenuEntryV6("Recientes", Icons.Default.History) { vm.openCategory("Recientes") },
            MenuEntryV6("Buscar", Icons.Default.Search) { vm.openSearch() },
            MenuEntryV6("Configuración", Icons.Default.Settings) { vm.go(Screen.SETTINGS) }
        )

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = if (phone) 10.dp else 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { if (ui.channels.isEmpty()) EmptyCatalogV6(ui, vm, phone) else HeroPanelV6(hero, ui, vm, phone, portrait) }
            item {
                Text("Explorar", fontSize = if (phone) 20.sp else 24.sp, fontWeight = FontWeight.Bold)
                Text("Botones grandes, rápidos y claros", color = V6SECOND, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) { items(menu, key = { it.label }) { MenuButtonV6(it, phone) } }
            }
            if (recent.isNotEmpty()) {
                item {
                    Text("Continúa viendo", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(recent, key = { it.id }) { RecentCardV6(it, vm, phone) } }
                }
            }
            item {
                val unique = ui.channels.distinctBy { it.id }.size
                Row(Modifier.fillMaxWidth().background(V6PANEL, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, null, tint = V6ACCENT); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$unique canales únicos", fontWeight = FontWeight.Bold)
                        Text("Menús, submenús, búsqueda, favoritos y recientes apuntan al mismo ID sin duplicar el catálogo.", color = V6SECOND, fontSize = 10.sp)
                    }
                    if (ui.refreshing) Text("Actualizando…", color = V6ACCENT, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogV6(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    Column(
        Modifier.fillMaxWidth().height(if (phone) 190.dp else 230.dp).background(V6PANEL, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(if (ui.refreshing) Icons.Default.Refresh else Icons.Default.TvOff, null, tint = V6ACCENT, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(8.dp))
        Text(if (ui.refreshing) "Preparando canales…" else "La app inició correctamente", fontWeight = FontWeight.Bold)
        Text(ui.statusMessage, color = V6SECOND, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        Button({ vm.refresh(false) }, enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(5.dp)); Text("Reintentar catálogo") }
    }
}

@Composable
private fun HeroPanelV6(channel: Channel?, ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val now = System.currentTimeMillis()
    val program = channel?.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(now) } }
    Card(Modifier.fillMaxWidth().height(if (phone && portrait) 190.dp else 230.dp), colors = CardDefaults.cardColors(containerColor = Color(0xD90B1422)), shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.fillMaxSize()) {
            channel?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .13f) }
            Column(Modifier.align(Alignment.BottomStart).padding(if (phone) 14.dp else 20.dp)) {
                Text(program?.title ?: channel?.let(::cleanNameV6).orEmpty().ifBlank { "TV Español+" }, fontSize = if (phone) 23.sp else 31.sp, fontWeight = FontWeight.Black, maxLines = 2)
                if (program != null) Text("● EN VIVO · ${cleanNameV6(channel)}", color = V6LIVE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                else channel?.let { Text("${ChannelClassifier.categoryFor(it)} · ${ChannelClassifier.countryName(it)}", color = V6SECOND, fontSize = 11.sp) }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ channel?.let(vm::play) }, enabled = channel != null) { Icon(Icons.Default.PlayArrow, null); Text(" Ver") }
                    OutlinedButton({ channel?.let(vm::startDual) }, enabled = channel != null) { Icon(Icons.Default.Fullscreen, null); Text(" Dual") }
                }
            }
        }
    }
}

@Composable
private fun MenuButtonV6(entry: MenuEntryV6, phone: Boolean) {
    FocusCardV6(entry.action, Modifier.width(if (phone) 118.dp else 154.dp).height(if (phone) 92.dp else 108.dp)) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(entry.icon, null, tint = V6TEXT, modifier = Modifier.size(if (phone) 30.dp else 38.dp))
            Spacer(Modifier.height(7.dp)); Text(entry.label, fontWeight = FontWeight.Bold, fontSize = if (phone) 11.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentCardV6(channel: Channel, vm: MainViewModel, phone: Boolean) {
    FocusCardV6({ vm.play(channel) }, Modifier.width(if (phone) 160.dp else 205.dp).height(if (phone) 70.dp else 82.dp)) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            channel.logo?.let { AsyncImage(it, null, Modifier.size(44.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(7.dp)) }
            Column(Modifier.weight(1f)) {
                Text(cleanNameV6(channel), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(ChannelClassifier.countryName(channel), color = V6SECOND, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChannelsScreenV6(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean, onExit: () -> Unit) {
    AppShellV6(phone, vm, ui.refreshing, onExit) {
        when {
            TextNormalizer.normalize(filter) == "paises" -> CountryFoldersV6(ui, vm, phone)
            filter.startsWith("País:") -> CountryCategoryFoldersV6(ui, filter.removePrefix("País:"), vm, phone)
            filter.startsWith("CountryCat:") -> {
                val payload = filter.removePrefix("CountryCat:")
                val country = payload.substringBefore('|')
                val category = payload.substringAfter('|', "TV general")
                val list = ChannelClassifier.channelsForCountry(ui.channels, country).filter { ChannelClassifier.categoryFor(it) == category }
                ChannelListWithPreviewV6("$country · $category", list, ui, vm, phone)
            }
            filter.startsWith("Subcat:") -> {
                val payload = filter.removePrefix("Subcat:")
                val category = payload.substringBefore('|')
                val sub = payload.substringAfter('|', "")
                ChannelListWithPreviewV6("$category · $sub", ChannelClassifier.channelsForSubcategory(ui.channels, category, sub), ui, vm, phone)
            }
            TextNormalizer.normalize(filter) == "favoritos" -> ChannelListWithPreviewV6("Favoritos", ui.channels.distinctBy { it.id }.filter { it.id in ui.favorites }, ui, vm, phone)
            TextNormalizer.normalize(filter) == "recientes" -> {
                val list = ui.recentIds.mapNotNull { id -> ui.channels.firstOrNull { it.id == id } }.distinctBy { it.id }
                ChannelListWithPreviewV6("Recientes", list, ui, vm, phone)
            }
            else -> {
                val subcategories = ChannelClassifier.subcategoriesForCategory(ui.channels, filter)
                if (subcategories.isNotEmpty()) SubcategoryFoldersV6(filter, subcategories, vm, phone)
                else ChannelListWithPreviewV6(filter, ChannelClassifier.channelsForCategory(ui.channels, filter), ui, vm, phone)
            }
        }
    }
}

@Composable
private fun CountryFoldersV6(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val countries = remember(ui.channels) { ChannelClassifier.countries(ui.channels) }
    FolderGridV6("Países", "Honduras aparece primero", countries.map { it.name to it.count }, phone) { vm.openCategory("País:$it") }
}

@Composable
private fun CountryCategoryFoldersV6(ui: AppUiState, country: String, vm: MainViewModel, phone: Boolean) {
    val countryChannels = remember(ui.channels, country) { ChannelClassifier.channelsForCountry(ui.channels, country) }
    val groups = remember(countryChannels) {
        ChannelClassifier.categories.mapNotNull { category ->
            val count = countryChannels.count { ChannelClassifier.categoryFor(it) == category.title }
            if (count > 0) category.title to count else null
        }
    }
    if (groups.size <= 1) ChannelListWithPreviewV6(country, countryChannels, ui, vm, phone)
    else FolderGridV6(country, "Elige el tipo de contenido", groups, phone) { vm.openCategory("CountryCat:$country|$it") }
}

@Composable
private fun SubcategoryFoldersV6(category: String, groups: List<SubcategoryGroup>, vm: MainViewModel, phone: Boolean) {
    FolderGridV6(category, "Submenús automáticos sin repetir canales", groups.map { it.name to it.count }, phone) { vm.openCategory("Subcat:$category|$it") }
}

@Composable
private fun FolderGridV6(title: String, subtitle: String, groups: List<Pair<String, Int>>, phone: Boolean, open: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Text(title, fontSize = if (phone) 22.sp else 27.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = V6SECOND, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(groups, key = { it.first }) { (name, count) ->
                FocusCardV6({ open(name) }, Modifier.fillMaxWidth().height(if (phone) 100.dp else 112.dp)) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Icon(if (TextNormalizer.normalize(name) == "honduras") Icons.Default.Public else Icons.Default.Folder, null, tint = V6ACCENT, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(6.dp)); Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count canales", color = V6SECOND, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelListWithPreviewV6(title: String, input: List<Channel>, ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val list = remember(input) { input.distinctBy { it.id } }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember(list) { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id, phone) {
        preview = null
        if (!phone && focused != null) { delay(900); preview = focused }
    }

    if (phone) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ListHeaderV6(title, list.size)
            focused?.let { CompactPreviewInfoV6(it, ui, vm) }
            Spacer(Modifier.height(7.dp))
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { channel -> ChannelCardV6(channel, ui, vm, true) { focused = channel } }
            }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.weight(.78f)) {
                ListHeaderV6(title, list.size); Spacer(Modifier.height(8.dp))
                PreviewPanelV6(focused, preview, ui, vm, Modifier.fillMaxWidth().weight(1f))
            }
            Spacer(Modifier.width(14.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { channel -> ChannelCardV6(channel, ui, vm, false) { focused = channel } }
            }
        }
    }
}

@Composable
private fun ListHeaderV6(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count canales", color = V6SECOND, fontSize = 10.sp)
    }
}

@Composable
private fun PreviewPanelV6(selected: Channel?, preview: Channel?, ui: AppUiState, vm: MainViewModel, modifier: Modifier) {
    Column(modifier.background(Color(0xD908111F), RoundedCornerShape(14.dp)).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
            if (preview != null) ResilientPlayer(preview, Modifier.fillMaxSize(), volume = 0f, controls = false)
            else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LiveTv, null, tint = V6SECOND, modifier = Modifier.size(46.dp)); Text("Mantén el foco para previsualizar", color = V6SECOND, fontSize = 10.sp)
            }
        }
        selected?.let { channel ->
            Spacer(Modifier.height(8.dp)); Text(cleanNameV6(channel), fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ChannelClassifier.categoryFor(channel)} · ${ChannelClassifier.countryName(channel)}", color = V6SECOND, fontSize = 10.sp)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button({ vm.play(channel) }, Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Text(" Ver") }
                OutlinedButton({ vm.startDual(channel) }, Modifier.weight(1f)) { Text("▣ Dual") }
                OutlinedButton({ vm.toggleFavorite(channel.id) }, Modifier.weight(1f)) { Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text(" Fav") }
            }
        }
    }
}

@Composable
private fun CompactPreviewInfoV6(channel: Channel, ui: AppUiState, vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().padding(top = 7.dp).background(Color(0xD908111F), RoundedCornerShape(10.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        channel.logo?.let { AsyncImage(it, null, Modifier.size(42.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(cleanNameV6(channel), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ChannelClassifier.categoryFor(channel)} · ${ChannelClassifier.countryName(channel)}", color = V6SECOND, fontSize = 9.sp, maxLines = 1)
        }
        IconButton({ vm.toggleFavorite(channel.id) }) { Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito") }
    }
}

@Composable
private fun ChannelCardV6(channel: Channel, ui: AppUiState, vm: MainViewModel, phone: Boolean, onFocus: () -> Unit) {
    FocusCardV6({ vm.play(channel) }, Modifier.fillMaxWidth().height(if (phone) 118.dp else 112.dp), onFocus) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (channel.logo != null) AsyncImage(channel.logo, null, Modifier.size(if (phone) 43.dp else 49.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Default.Tv, null, tint = V6SECOND, modifier = Modifier.size(43.dp))
                Spacer(Modifier.width(7.dp))
                Text(cleanNameV6(channel), Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = if (phone) 11.sp else 13.sp)
                IconButton({ vm.toggleFavorite(channel.id) }, Modifier.size(32.dp)) {
                    Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (channel.id in ui.favorites) V6LIVE else V6SECOND, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp)); Text(ChannelClassifier.countryName(channel), color = V6SECOND, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlayerScreenV6(ui: AppUiState, prefs: UserPreferences, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val channel = ui.selectedChannel ?: return
    var error by remember(channel.id) { mutableStateOf<String?>(null) }
    var showTracks by remember(channel.id) { mutableStateOf(false) }
    var retryToken by remember(channel.id) { mutableIntStateOf(0) }
    val program = channel.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(System.currentTimeMillis()) } }

    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Black) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ResilientPlayer(
                channel = channel,
                modifier = Modifier.fillMaxSize(),
                volume = 1f,
                controls = true,
                showTrackMenu = showTracks,
                onTrackMenuDismiss = { showTracks = false },
                onTerminalError = { error = it },
                preferredAudioLanguage = prefs.preferredAudioLanguage,
                preferredSubtitleLanguage = prefs.preferredSubtitleLanguage,
                subtitleTextSizeSp = prefs.subtitleTextSizeSp,
                retryToken = retryToken
            )
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.78f)).padding(8.dp)) {
                Text(program?.title ?: cleanNameV6(channel), color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (phone) 14.sp else 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${cleanNameV6(channel)} · ${ChannelClassifier.countryName(channel)}", color = V6SECOND, fontSize = 9.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PlayerButtonV6(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (phone && portrait) "Fav" else "Favorito", Modifier.weight(1f)) { vm.toggleFavorite(channel.id) }
                    PlayerButtonV6(Icons.Default.Language, if (phone && portrait) "Idioma" else "Audio/Sub", Modifier.weight(1f)) { showTracks = true }
                    PlayerButtonV6(Icons.Default.Fullscreen, if (phone && portrait) "Dual" else "Vista doble", Modifier.weight(1f)) { vm.startDual(channel) }
                    PlayerButtonV6(Icons.Default.Home, "Volver", Modifier.weight(1f)) { vm.go(Screen.HOME) }
                }
            }
            error?.let { message ->
                Column(Modifier.align(Alignment.Center).background(Color(0xEE111A27), RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, color = Color.White); Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ error = null; retryToken += 1 }) { Icon(Icons.Default.Refresh, null); Text(" Reintentar") }
                        OutlinedButton({ vm.go(Screen.HOME) }) { Text("Volver") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerButtonV6(icon: ImageVector, label: String, modifier: Modifier, click: () -> Unit) {
    OutlinedButton(click, modifier.height(48.dp)) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(label, maxLines = 1) }
}

@Composable
private fun SettingsScreenV6(ui: AppUiState, prefs: UserPreferences, parentalUnlocked: Boolean, vm: MainViewModel, phone: Boolean, onExit: () -> Unit) {
    var showPinSetup by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    AppShellV6(phone, vm, ui.refreshing, onExit) {
        LazyColumn(Modifier.fillMaxSize().padding(if (phone) 10.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { Text("Configuración", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Ajustes simples, visibles y persistentes", color = V6SECOND, fontSize = 10.sp) }
            item { SettingCardV6(Icons.Default.Tv, "Mis listas", "Agregar, seleccionar y actualizar listas") { vm.go(Screen.PLAYLISTS) } }
            item { SettingCardV6(Icons.Default.Refresh, "Actualizar ahora", "Mantiene la última lista si Internet falla") { vm.refresh(false) } }
            item { SettingCardV6(Icons.Default.Search, "Súper Búsqueda", "Acentos, errores de escritura, coincidencias parciales y EPG") { vm.openSearch() } }
            item { SettingCardV6(Icons.Default.Fullscreen, "Vista doble", "Dos canales simultáneos con un solo audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) } }
            item { SettingsSectionTitleV6("Audio y subtítulos") }
            item { ChoiceSettingV6("Idioma de audio", listOf("Español Latino" to "es-419", "Español" to "es", "Inglés" to "en", "Portugués" to "pt"), prefs.preferredAudioLanguage, vm::setPreferredAudioLanguage) }
            item { ChoiceSettingV6("Idioma de subtítulos", listOf("Español" to "es", "Inglés" to "en", "Portugués" to "pt", "Desactivados" to "off"), prefs.preferredSubtitleLanguage, vm::setPreferredSubtitleLanguage) }
            item { ChoiceSettingV6("Tamaño de subtítulos", listOf("Pequeño" to "16", "Normal" to "20", "Grande" to "24", "Extra grande" to "28"), prefs.subtitleTextSizeSp.toInt().toString()) { vm.setSubtitleTextSize(it.toFloat()) } }
            item { SettingsSectionTitleV6("Pantalla y accesibilidad") }
            item { ChoiceSettingV6("Tamaño del texto", listOf("Compacto" to "0.9", "Normal" to "1.0", "Grande" to "1.15", "Extra grande" to "1.3"), closestTextScaleV6(prefs.uiTextScale)) { vm.setUiTextScale(it.toFloat()) } }
            item { SettingsSectionTitleV6("Control parental") }
            item {
                Column(Modifier.fillMaxWidth().background(V6PANEL, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = V6ACCENT); Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Protección con PIN", fontWeight = FontWeight.Bold)
                            Text(if (prefs.parentalPinHash.isBlank()) "Configura un PIN de 4 a 6 dígitos" else if (parentalUnlocked) "Desbloqueado para esta sesión" else "Adultos 18+ y canales bloqueados requieren PIN", color = V6SECOND, fontSize = 10.sp)
                        }
                        Switch(prefs.parentalEnabled, { vm.setParentalEnabled(it) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ showPinSetup = true }, Modifier.weight(1f)) { Icon(Icons.Default.Lock, null); Text(if (prefs.parentalPinHash.isBlank()) " Crear PIN" else " Cambiar PIN") }
                        OutlinedButton({ vm.requestParentalUnlock() }, Modifier.weight(1f), enabled = prefs.parentalEnabled && !parentalUnlocked) { Text("Desbloquear") }
                    }
                    ui.selectedChannel?.let { selected ->
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton({ vm.toggleChannelLock(selected.id) }, Modifier.fillMaxWidth()) {
                            val locked = selected.id in prefs.lockedChannelIds
                            Icon(if (locked) Icons.Default.Lock else Icons.Default.Security, null)
                            Text(if (locked) " Quitar bloqueo de ${cleanNameV6(selected)}" else " Bloquear ${cleanNameV6(selected)}")
                        }
                    }
                    if (prefs.lockedChannelIds.isNotEmpty()) Text("${prefs.lockedChannelIds.size} canal(es) bloqueados manualmente", color = V6SECOND, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            item { SettingCardV6(Icons.Default.Description, "Acerca de", "Versión 1.5.0 · Android TV y móvil") { vm.go(Screen.ABOUT) } }
        }
    }

    if (showPinSetup) {
        AlertDialog(
            onDismissRequest = { showPinSetup = false; newPin = "" },
            title = { Text(if (prefs.parentalPinHash.isBlank()) "Crear PIN parental" else "Cambiar PIN parental") },
            text = {
                OutlinedTextField(
                    newPin,
                    { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("PIN de 4 a 6 dígitos") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = { TextButton({ if (vm.setParentalPin(newPin)) { showPinSetup = false; newPin = "" } }) { Text("Guardar") } },
            dismissButton = { TextButton({ showPinSetup = false; newPin = "" }) { Text("Cancelar") } }
        )
    }
}

@Composable private fun SettingsSectionTitleV6(title: String) { Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }

@Composable
private fun SettingCardV6(icon: ImageVector, title: String, subtitle: String, click: () -> Unit) {
    FocusCardV6(click, Modifier.fillMaxWidth().height(76.dp)) {
        Row(Modifier.fillMaxSize().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = V6ACCENT, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = V6SECOND, fontSize = 10.sp, maxLines = 2) }
        }
    }
}

@Composable
private fun ChoiceSettingV6(title: String, choices: List<Pair<String, String>>, selected: String, change: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(V6PANEL, RoundedCornerShape(12.dp)).padding(11.dp)) {
        Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(choices, key = { it.second }) { (label, value) ->
                if (selected == value) Button({ change(value) }) { Text(label, fontSize = 10.sp) }
                else OutlinedButton({ change(value) }) { Text(label, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun AboutScreenV6(phone: Boolean) {
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Transparent) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 88.dp else 120.dp))
            Spacer(Modifier.height(12.dp)); Text("TV Español+ Player", fontSize = if (phone) 25.sp else 34.sp, fontWeight = FontWeight.Black)
            Text("Versión 1.5.0", color = V6ACCENT, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            Text("Android TV · Google TV · Android móvil · Kotlin · Media3", color = V6SECOND, fontSize = 10.sp)
            Text("Responsive · Vista doble · búsqueda inteligente · control parental", color = V6SECOND, fontSize = 10.sp)
            Spacer(Modifier.height(14.dp)); Text("Honduras", color = Color(0xFF00A3E0), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ParentalPinDialogV6(gate: ParentalGate, vm: MainViewModel) {
    var pin by remember(gate) { mutableStateOf("") }
    var invalid by remember(gate) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = vm::dismissParentalGate,
        title = { Text(gate.title) },
        text = {
            Column {
                Text(gate.message); Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    pin,
                    { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; invalid = false } },
                    label = { Text("PIN parental") },
                    isError = invalid,
                    supportingText = { if (invalid) Text("PIN incorrecto") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton({ if (!vm.submitParentalPin(pin)) invalid = true }) { Text("Desbloquear") } },
        dismissButton = { TextButton(vm::dismissParentalGate) { Text("Cancelar") } }
    )
}

@Composable
private fun ErrorBannerV6(message: String, close: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(10.dp).background(Color(0xEE3A1720), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), fontSize = 10.sp); IconButton(close) { Icon(Icons.Default.Close, "Cerrar") }
    }
}

@Composable
private fun FocusCardV6(onClick: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocus() }
    val border by animateColorAsState(if (focused) V6ACCENT else Color.Transparent, label = "v6-focus")
    Card(
        modifier = modifier.scale(if (focused) 1.025f else 1f).border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(12.dp)).clickable(interactionSource = interaction, indication = null, onClick = onClick).focusable(interactionSource = interaction),
        colors = CardDefaults.cardColors(containerColor = if (focused) V6PANEL2 else V6PANEL),
        shape = RoundedCornerShape(12.dp)
    ) { content() }
}

private fun cleanNameV6(channel: Channel): String = channel.name.replace(Regex("\\s*\\((?:[0-9]{3,4}p|HD|FHD|UHD|4K)\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()

private fun closestTextScaleV6(value: Float): String {
    val options = listOf(0.9f, 1.0f, 1.15f, 1.3f)
    return options.minByOrNull { abs(it - value) }?.toString() ?: "1.0"
}
