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

private val CBG = Color(0xFF101114)
private val CPANEL = Color(0xFF191B20)
private val CPANEL2 = Color(0xFF23262D)
private val CAC = Color(0xFF19B5A5)
private val CLIVE = Color(0xFFE53935)
private val CTEXT = Color(0xFFF7F8FA)
private val CSECOND = Color(0xFFB8BBC3)

@Composable
fun TVEspanolPlusRootV3(vm: MainViewModel, onExit: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val screen by vm.screen.collectAsState()
    val filter by vm.filter.collectAsState()
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current
    val phone = !remember(ctx) { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val portrait = cfg.screenHeightDp >= cfg.screenWidthDp
    var confirmExit by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = CAC, background = CBG, surface = CPANEL, onSurface = CTEXT, onBackground = CTEXT)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = CBG) {
            if (ui.loading && ui.channels.isEmpty()) {
                LoadingV3(ui.statusMessage, phone)
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (screen != Screen.PLAYER && screen != Screen.DUAL) {
                        HeaderV3(phone, ui.refreshing, { vm.go(Screen.HOME) }, vm::openSearch, { vm.refresh(false) }, { vm.go(Screen.SETTINGS) }, { confirmExit = true })
                    }
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.HOME -> HomeV3(ui, vm, phone, portrait)
                            Screen.CHANNELS -> ChannelsV3(ui, filter, vm, phone)
                            Screen.SEARCH -> SearchV3(ui, vm, phone)
                            Screen.PLAYER -> PlayerV3(ui, vm, phone, portrait)
                            Screen.DUAL -> DualV3(ui, vm, phone, portrait)
                            Screen.PLAYLISTS -> PlaylistsV3(ui, vm, phone)
                            Screen.SETTINGS -> SettingsV3(ui, vm, phone)
                            Screen.ABOUT -> AboutV3(phone)
                        }
                        ui.error?.let { ErrorBarV3(it, { vm.clearError() }, Modifier.align(Alignment.BottomCenter)) }
                    }
                    if (phone && screen != Screen.PLAYER && screen != Screen.DUAL) BottomV3(vm, screen)
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
private fun BrandV3(phone: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 32.dp else 50.dp))
        Spacer(Modifier.width(4.dp))
        Text("TV Español", fontSize = if (phone) 16.sp else 27.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text("+", fontSize = if (phone) 19.sp else 31.sp, fontWeight = FontWeight.Black, color = CAC)
    }
}

@Composable
private fun LoadingV3(message: String, phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandV3(phone); Spacer(Modifier.height(18.dp)); CircularProgressIndicator(color = CAC); Spacer(Modifier.height(12.dp)); Text(message, color = CSECOND); Spacer(Modifier.height(18.dp)); LegalV3()
    }
}

@Composable
private fun HeaderV3(phone: Boolean, refreshing: Boolean, home: () -> Unit, search: () -> Unit, refresh: () -> Unit, settings: () -> Unit, exit: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (phone) 54.dp else 72.dp).background(CPANEL).padding(horizontal = if (phone) 5.dp else 18.dp), verticalAlignment = Alignment.CenterVertically) {
        BrandV3(phone, Modifier.clickable(onClick = home)); Spacer(Modifier.weight(1f))
        if (phone) {
            SmallTopV3(Icons.Default.Search, "Buscar", search)
            SmallTopV3(Icons.Default.Refresh, "Actualizar", refresh, !refreshing)
            SmallTopV3(Icons.Default.Settings, "Configuración", settings)
            SmallTopV3(Icons.Default.ExitToApp, "Salir", exit)
        } else {
            HeaderButtonV3("Buscar", Icons.Default.Search, search)
            HeaderButtonV3(if (refreshing) "Actualizando" else "Actualizar", Icons.Default.Refresh, refresh, !refreshing)
            HeaderButtonV3("Configuración", Icons.Default.Settings, settings)
            HeaderButtonV3("Salir", Icons.Default.ExitToApp, exit)
        }
    }
}

@Composable private fun SmallTopV3(icon: ImageVector, desc: String, action: () -> Unit, enabled: Boolean = true) {
    IconButton(action, enabled = enabled, modifier = Modifier.size(36.dp)) { Icon(icon, desc, Modifier.size(21.dp)) }
}

@Composable private fun HeaderButtonV3(text: String, icon: ImageVector, action: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(action, enabled = enabled, modifier = Modifier.padding(start = 7.dp)) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(text) }
}

@Composable
private fun BottomV3(vm: MainViewModel, screen: Screen) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(CPANEL), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BottomActionV3("Inicio", Icons.Default.Home, screen == Screen.HOME) { vm.go(Screen.HOME) }
        BottomActionV3("Buscar", Icons.Default.Search, screen == Screen.SEARCH) { vm.openSearch() }
        BottomActionV3("Favoritos", Icons.Default.Favorite, false) { vm.openCategory("Favoritos") }
        BottomActionV3("Más", Icons.Default.Settings, screen == Screen.SETTINGS) { vm.go(Screen.SETTINGS) }
    }
}

@Composable private fun BottomActionV3(label: String, icon: ImageVector, selected: Boolean, action: () -> Unit) {
    TextButton(action) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, label, tint = if (selected) CAC else CSECOND, modifier = Modifier.size(19.dp)); Text(label, color = if (selected) CAC else CSECOND, fontSize = 9.sp) } }
}

@Composable
private fun HomeV3(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val now = System.currentTimeMillis()
    val live = remember(ui.channels, ui.programs, now / 60_000L) {
        ui.channels.mapNotNull { c -> c.tvgId?.let { id -> ui.programs[id]?.firstOrNull { it.isLive(now) }?.let { p -> c to p } } }.distinctBy { it.first.id }.take(8)
    }
    val recent = remember(ui.recentIds, ui.channels) { ui.recentIds.mapNotNull { id -> ui.channels.find { it.id == id } }.take(8) }
    val hero = live.firstOrNull()?.first ?: recent.firstOrNull() ?: ui.channels.firstOrNull()
    val heroProgram = live.firstOrNull()?.second
    val counts = remember(ui.channels) { ChannelClassifier.categoryCounts(ui.channels) }
    val countryCount = remember(ui.channels) { ChannelClassifier.countries(ui.channels).size }

    if (phone && portrait) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { HeroV3(hero, heroProgram, vm, Modifier.fillMaxWidth().height(190.dp), true) }
            if (live.isNotEmpty()) {
                item { SectionV3("Ahora en vivo", "Lo más cercano a la fecha y hora actuales") }
                items(live, key = { it.first.id }) { (c, p) -> LiveRowV3(c, p) { vm.play(c) } }
            }
            if (recent.isNotEmpty()) {
                item { SectionV3("Continúa viendo", "Tus últimos canales") }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(recent, key = { it.id }) { c -> MiniV3(c) { vm.play(c) } } } }
            }
            item { SectionV3("Categorías", "Cada canal pertenece a una sola categoría") }
            val cats = ChannelClassifier.categories.filter { (counts[it.title] ?: 0) > 0 }
            items(cats.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { cat -> CategoryCardV3(cat.title, counts[cat.title] ?: 0, vm, Modifier.weight(1f).height(92.dp), true) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecialCardV3("Países", "$countryCount países", Icons.Default.Public, { vm.openCategory("Países") }, Modifier.weight(1f).height(92.dp), true)
                    SpecialCardV3("Favoritos", "${ui.favorites.size} canales", Icons.Default.Favorite, { vm.openCategory("Favoritos") }, Modifier.weight(1f).height(92.dp), true)
                }
            }
            item { RefreshInfoV3(ui) }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(if (phone) 12.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth().weight(.52f)) {
                HeroV3(hero, heroProgram, vm, Modifier.weight(1.4f).fillMaxHeight(), phone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { SectionV3("Ahora en vivo", "Prioridad por fecha y hora"); live.take(4).forEach { (c, p) -> LiveRowV3(c, p) { vm.play(c) } } }
            }
            Spacer(Modifier.height(10.dp)); SectionV3("Categorías", "Sin canales repetidos entre categorías")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChannelClassifier.categories.filter { (counts[it.title] ?: 0) > 0 }) { cat -> CategoryCardV3(cat.title, counts[cat.title] ?: 0, vm, Modifier.width(170.dp).height(94.dp), phone) }
                item { SpecialCardV3("Países", "$countryCount países", Icons.Default.Public, { vm.openCategory("Países") }, Modifier.width(170.dp).height(94.dp), phone) }
                item { SpecialCardV3("Favoritos", "${ui.favorites.size} canales", Icons.Default.Favorite, { vm.openCategory("Favoritos") }, Modifier.width(170.dp).height(94.dp), phone) }
            }
            RefreshInfoV3(ui)
        }
    }
}

@Composable
private fun HeroV3(channel: Channel?, program: Program?, vm: MainViewModel, modifier: Modifier, phone: Boolean) {
    FocusCardV3({ channel?.let(vm::play) }, modifier) {
        Box(Modifier.fillMaxSize()) {
            channel?.logo?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .18f) }
            Column(Modifier.align(Alignment.BottomStart).padding(if (phone) 14.dp else 20.dp)) {
                val channelName = channel?.let(::cleanNameV3).orEmpty()
                val title = program?.title?.takeIf { TextNormalizer.normalize(it) != TextNormalizer.normalize(channelName) } ?: channelName.ifBlank { "TV Español+" }
                Text(title, fontSize = if (phone) 24.sp else 30.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                if (program != null) {
                    Text("● EN VIVO", color = CLIVE, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(channelName, color = CSECOND, fontSize = 12.sp)
                } else {
                    Text(channel?.let { ChannelClassifier.categoryFor(it) }.orEmpty(), color = CSECOND, fontSize = 11.sp)
                }
                qualityV3(channel?.name.orEmpty())?.let { Text(it, color = CSECOND, fontSize = 10.sp) }
                Spacer(Modifier.height(5.dp)); Text(if (phone) "Toca para ver" else "OK para ver", color = CAC, fontSize = 11.sp)
            }
        }
    }
}

@Composable private fun SectionV3(title: String, subtitle: String? = null) { Column { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, color = CSECOND, fontSize = 11.sp) } } }

@Composable private fun LiveRowV3(channel: Channel, program: Program, click: () -> Unit) {
    FocusCardV3(click, Modifier.fillMaxWidth().height(62.dp).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(program.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold); Text("● EN VIVO · ${cleanNameV3(channel)}", color = CSECOND, fontSize = 10.sp, maxLines = 1) }
            Text("VER", color = CAC, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun CategoryCardV3(title: String, count: Int, vm: MainViewModel, modifier: Modifier, phone: Boolean) = SpecialCardV3(title, "$count canales", categoryIconV3(title), { vm.openCategory(title) }, modifier, phone)

@Composable
private fun SpecialCardV3(title: String, subtitle: String, icon: ImageVector, click: () -> Unit, modifier: Modifier, phone: Boolean) {
    FocusCardV3(click, modifier) {
        Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 13.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = CAC, modifier = Modifier.size(if (phone) 24.dp else 28.dp)); Spacer(Modifier.height(4.dp)); Text(title, fontWeight = FontWeight.SemiBold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, color = CSECOND, fontSize = 10.sp)
        }
    }
}

private fun categoryIconV3(title: String): ImageVector = when (title) {
    "Deportes" -> Icons.Default.SportsSoccer; "Películas" -> Icons.Default.Movie; "Series" -> Icons.Default.Tv; "Cristianos" -> Icons.Default.Church; "Noticias" -> Icons.Default.Article; "Niños y familia" -> Icons.Default.ChildCare; "Música" -> Icons.Default.MusicNote; "Documentales y cultura" -> Icons.Default.Description; "Entretenimiento" -> Icons.Default.TheaterComedy; "TV general" -> Icons.Default.LiveTv; "Adultos 18+" -> Icons.Default.Lock; else -> Icons.Default.Category
}

@Composable private fun RefreshInfoV3(ui: AppUiState) { ui.lastRefresh?.let { r -> Spacer(Modifier.height(6.dp)); Text("✓ ${r.channelCount} canales · +${r.added} · -${r.removed} · ${r.changed} modificados", color = CSECOND, fontSize = 10.sp) } }

@Composable
private fun ChannelsV3(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean) {
    if (TextNormalizer.normalize(filter) == "paises") { CountriesV3(ui, vm, phone); return }
    val list = remember(ui.channels, ui.favorites, filter) {
        when {
            TextNormalizer.normalize(filter) == "favoritos" -> ui.channels.distinctBy { it.id }.filter { it.id in ui.favorites }
            filter.startsWith("País:") -> ChannelClassifier.channelsForCountry(ui.channels, filter.removePrefix("País:"))
            else -> ChannelClassifier.channelsForCategory(ui.channels, filter)
        }
    }
    var focused by remember(list) { mutableStateOf(list.firstOrNull()) }
    var preview by remember { mutableStateOf<Channel?>(null) }
    LaunchedEffect(focused?.id, phone) { preview = null; if (!phone) { delay(1200); preview = focused } }
    val title = filter.removePrefix("País:")

    if (phone) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ListHeaderV3(title, list.size); Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { c -> ChannelCardV3(c, ui, vm, Modifier.fillMaxWidth().height(116.dp), true) { focused = c } }
            }
        }
    } else {
        Row(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.weight(.82f)) {
                ListHeaderV3(title, list.size); Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                    preview?.let { ResilientPlayer(it, Modifier.fillMaxSize(), volume = 0f, controls = false) }
                    if (preview == null) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Tv, null, tint = CSECOND, modifier = Modifier.size(42.dp)); Text("Mantén el foco para previsualizar", color = CSECOND) }
                }
                focused?.let { Spacer(Modifier.height(6.dp)); Text(cleanNameV3(it), fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(14.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { c -> ChannelCardV3(c, ui, vm, Modifier.fillMaxWidth().height(106.dp), false) { focused = c } }
            }
        }
    }
}

@Composable private fun ListHeaderV3(title: String, count: Int) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("$count canales", color = CSECOND, fontSize = 11.sp) } }

@Composable
private fun CountriesV3(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val countries = remember(ui.channels) { ChannelClassifier.countries(ui.channels) }
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Países", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Un folder por país", color = CSECOND, fontSize = 11.sp) }; Text("${countries.size} países", color = CSECOND, fontSize = 11.sp) }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(countries, key = { it.name }) { country ->
                FocusCardV3({ vm.openCategory("País:${country.name}") }, Modifier.fillMaxWidth().height(if (phone) 96.dp else 104.dp)) {
                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Folder, null, tint = if (TextNormalizer.normalize(country.name) == "honduras") CAC else CSECOND, modifier = Modifier.size(27.dp)); Spacer(Modifier.width(7.dp)); Text(country.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)) }
                        Spacer(Modifier.height(5.dp)); Text("${country.count} canales", color = CSECOND, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelCardV3(c: Channel, ui: AppUiState, vm: MainViewModel, modifier: Modifier, phone: Boolean, onFocus: () -> Unit) {
    FocusCardV3({ vm.play(c) }, modifier, onFocus) {
        Column(Modifier.fillMaxSize().padding(if (phone) 8.dp else 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(if (phone) 42.dp else 48.dp), contentScale = ContentScale.Fit) else Box(Modifier.size(if (phone) 42.dp else 48.dp).background(CPANEL2, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text(cleanNameV3(c).take(2).uppercase(), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(7.dp)); Text(cleanNameV3(c), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = if (phone) 12.sp else 14.sp)
                IconButton({ vm.toggleFavorite(c.id) }, modifier = Modifier.size(34.dp)) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (c.id in ui.favorites) CLIVE else CSECOND, modifier = Modifier.size(18.dp)) }
            }
            Row { Text(ChannelClassifier.categoryFor(c), color = CSECOND, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1); qualityV3(c.name)?.let { Text(it, color = CSECOND, fontSize = 9.sp) } }
        }
    }
}

@Composable
private fun SearchV3(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        OutlinedTextField(ui.searchQuery, vm::search, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (ui.searchQuery.isNotBlank()) IconButton({ vm.search("") }) { Icon(Icons.Default.Close, "Limpiar") } }, label = { Text("Súper Búsqueda") }, placeholder = { Text("fc barcelos, Fórmula 1, noticias Honduras") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
        Spacer(Modifier.height(8.dp))
        if (ui.searchQuery.length < 2) {
            Text("Busca canales, programas y eventos. Prioriza lo más cercano a la fecha y hora actuales.", color = CSECOND, fontSize = 11.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ui.searchResults, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                    FocusCardV3({ vm.play(hit.channel) }, Modifier.fillMaxWidth().height(if (phone) 80.dp else 84.dp)) {
                        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            hit.channel.logo?.let { AsyncImage(it, null, Modifier.size(if (phone) 42.dp else 52.dp)) }; Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(hit.program?.title ?: cleanNameV3(hit.channel), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(cleanNameV3(hit.channel), color = CSECOND, fontSize = 10.sp, maxLines = 1) }
                            Column(horizontalAlignment = Alignment.End) { Text(if (hit.temporalBucket == TemporalBucket.LIVE_NOW) "● EN VIVO" else hit.reason.uppercase(), color = if (hit.temporalBucket == TemporalBucket.LIVE_NOW) CLIVE else CAC, fontSize = 9.sp, fontWeight = FontWeight.Bold); TextButton({ vm.startDual(hit.channel) }) { Text("▣ Dual", fontSize = 10.sp) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun MiniV3(c: Channel, click: () -> Unit) { FocusCardV3(click, Modifier.width(160.dp).height(66.dp)) { Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) { c.logo?.let { AsyncImage(it, null, Modifier.size(36.dp)) }; Spacer(Modifier.width(6.dp)); Text(cleanNameV3(c), maxLines = 2, fontSize = 11.sp) } } }

@Composable
private fun PlayerV3(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val c = ui.selectedChannel ?: return
    var error by remember(c.id) { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ResilientPlayer(c, Modifier.fillMaxSize(), 1f, true) { error = it }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.76f)).padding(7.dp)) {
            Text(cleanNameV3(c), fontWeight = FontWeight.Bold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1); Text(ChannelClassifier.categoryFor(c), color = CSECOND, fontSize = 10.sp); Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton({ vm.toggleFavorite(c.id) }, Modifier.weight(1f)) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(17.dp)); if (!phone || !portrait) Text(" Favorito") }
                OutlinedButton({ vm.startDual(c) }, Modifier.weight(1f)) { Text(if (phone && portrait) "▣ Dual" else "▣ Vista doble") }
                OutlinedButton({ vm.go(Screen.HOME) }, Modifier.weight(1f)) { Text("Volver") }
            }
        }
        error?.let { Text(it, Modifier.align(Alignment.Center).background(Color.Black.copy(.82f), RoundedCornerShape(8.dp)).padding(14.dp)) }
    }
}

@Composable
private fun DualV3(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    if (phone && portrait) Column(Modifier.fillMaxSize().background(Color.Black)) { DualPaneV3(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), true); Box(Modifier.fillMaxWidth().height(2.dp).background(CAC)); DualPaneV3(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), true) }
    else Row(Modifier.fillMaxSize().background(Color.Black)) { DualPaneV3(DualSide.LEFT, ui.dualLeft, ui, ui.dualAudioSide == DualSide.LEFT, vm, Modifier.weight(1f), phone); Box(Modifier.width(2.dp).fillMaxHeight().background(CAC)); DualPaneV3(DualSide.RIGHT, ui.dualRight, ui, ui.dualAudioSide == DualSide.RIGHT, vm, Modifier.weight(1f), phone) }
}

@Composable
private fun DualPaneV3(side: DualSide, channel: Channel?, ui: AppUiState, audio: Boolean, vm: MainViewModel, modifier: Modifier, compact: Boolean) {
    var q by remember(side) { mutableStateOf("") }; var results by remember(side) { mutableStateOf<List<SearchHit>>(emptyList()) }
    LaunchedEffect(q, ui.channels, ui.programs) { delay(220); results = if (q.length >= 2) SearchEngine.search(q, ui.channels, ui.programs, 25) else emptyList() }
    Column(modifier.fillMaxHeight().background(if (audio) Color(0xFF071C1A) else Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(q, { q = it }, Modifier.weight(1f), leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }, placeholder = { Text(if (side == DualSide.LEFT) "Buscar izquierda…" else "Buscar derecha…", fontSize = if (compact) 9.sp else 11.sp) }, singleLine = true)
            IconButton({ vm.setDualAudio(side) }) { Text(if (audio) "🔊" else "🔇") }; IconButton({ vm.fullScreenFromDual(side) }, enabled = channel != null) { Icon(Icons.Default.Fullscreen, "Agrandar") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (channel != null) { ResilientPlayer(channel, Modifier.fillMaxSize(), if (audio) 1f else 0f, false); Text(cleanNameV3(channel), Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(.65f)).padding(5.dp), fontSize = if (compact) 9.sp else 11.sp, maxLines = 1) } else Text("Busca un canal", Modifier.align(Alignment.Center), color = CSECOND)
            if (q.length >= 2) LazyColumn(Modifier.fillMaxSize().background(Color(0xEE101114)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(results, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit -> FocusCardV3({ vm.setDualChannel(side, hit.channel); q = "" }, Modifier.fillMaxWidth().height(50.dp)) { Row(Modifier.fillMaxSize().padding(5.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(hit.program?.title ?: cleanNameV3(hit.channel), maxLines = 1, fontSize = 10.sp); Text(cleanNameV3(hit.channel), color = CSECOND, fontSize = 8.sp, maxLines = 1) }; if (hit.temporalBucket == TemporalBucket.LIVE_NOW) Text("●", color = CLIVE) } } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { IconButton({ vm.swapDual() }) { Icon(Icons.Default.SwapHoriz, "Intercambiar") }; IconButton({ vm.fullScreenFromDual(side) }, enabled = channel != null) { Icon(Icons.Default.Fullscreen, "Agrandar") }; IconButton({ vm.closeDualSide(side) }) { Icon(Icons.Default.Close, "Cerrar") } }
    }
}

@Composable
private fun PlaylistsV3(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    var name by remember { mutableStateOf("") }; var url by remember { mutableStateOf("") }; var editId by remember { mutableStateOf<String?>(null) }
    val clear = { name = ""; url = ""; editId = null }
    Column(Modifier.fillMaxSize().padding(if (phone) 12.dp else 18.dp)) {
        Text("Mis listas", fontSize = 23.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(ui.playlists, key = { it.id }) { p -> PlaylistCardV3(p, vm, { if (p.id != LocalStore.DEFAULT_ID) { name = p.name; url = p.url; editId = p.id } }) }
        }
        Text(if (editId == null) "Agregar lista" else "Editar lista", fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true); Spacer(Modifier.height(5.dp)); OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("URL M3U") }, singleLine = true); Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button({ if (editId == null) vm.addPlaylist(name, url) else vm.editPlaylist(editId!!, name, url); clear() }, Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text(" Guardar") }
            if (editId != null && editId != LocalStore.DEFAULT_ID) OutlinedButton({ vm.deletePlaylist(editId!!); clear() }, Modifier.weight(1f)) { Icon(Icons.Default.Delete, null); Text(" Eliminar") }
            OutlinedButton({ vm.refresh(false) }, Modifier.weight(1f), enabled = !ui.refreshing) { Icon(Icons.Default.Refresh, null); Text(" Actualizar") }
        }
        Text("La lista predeterminada está protegida y no se puede eliminar ni modificar.", color = CSECOND, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PlaylistCardV3(p: PlaylistConfig, vm: MainViewModel, edit: () -> Unit) {
    val protected = p.id == LocalStore.DEFAULT_ID
    FocusCardV3(edit, Modifier.fillMaxWidth().height(82.dp)) {
        Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text((if (p.active) "✓ " else "") + p.name, fontWeight = FontWeight.Bold, maxLines = 1); if (protected) Text("Predeterminada · protegida", color = CAC, fontSize = 9.sp); Text(p.url, color = CSECOND, fontSize = 8.sp, maxLines = 1) }
            if (protected) Icon(Icons.Default.Lock, "Protegida", tint = CAC); if (!p.active) TextButton({ vm.activatePlaylist(p.id) }) { Text("Usar") }
        }
    }
}

@Composable
private fun SettingsV3(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(if (phone) 12.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Configuración", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        item { SettingV3(Icons.Default.Tv, "Mis listas", "Agregar, seleccionar y actualizar") { vm.go(Screen.PLAYLISTS) } }
        item { SettingV3(Icons.Default.Refresh, "Actualización", "Al abrir · cada 6 horas · al recuperar Internet") { vm.refresh(false) } }
        item { SettingV3(Icons.Default.Search, "Súper Búsqueda", "Acentos, fuzzy y prioridad por fecha/hora") { vm.go(Screen.SEARCH) } }
        item { SettingV3(Icons.Default.Fullscreen, "Vista doble", "Dos canales, dos buscadores, un audio activo") { ui.selectedChannel?.let(vm::startDual) ?: ui.channels.firstOrNull()?.let(vm::startDual) } }
        item { SettingV3(Icons.Default.Article, "Acerca de", "Versión, tecnología y contacto") { vm.go(Screen.ABOUT) } }
        item { StatusV3(ui) }
    }
}

@Composable private fun SettingV3(icon: ImageVector, title: String, subtitle: String, click: () -> Unit) { FocusCardV3(click, Modifier.fillMaxWidth().height(68.dp)) { Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = CAC); Spacer(Modifier.width(9.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = CSECOND, fontSize = 10.sp) } } } }

@Composable private fun StatusV3(ui: AppUiState) { Column(Modifier.fillMaxWidth().background(CPANEL, RoundedCornerShape(12.dp)).padding(14.dp)) { Text("Estado", fontWeight = FontWeight.Bold, fontSize = 18.sp); StatusRowV3("Lista", ui.activePlaylist?.name ?: "—"); StatusRowV3("Canales", ui.channels.size.toString()); StatusRowV3("Categorías", ChannelClassifier.categories.count { ChannelClassifier.categoryCount(ui.channels, it.title) > 0 }.toString()); StatusRowV3("Países", ChannelClassifier.countries(ui.channels).size.toString()); StatusRowV3("EPG", if (ui.programs.isNotEmpty()) "Disponible" else "Sin datos"); Spacer(Modifier.height(8.dp)); LegalV3() } }
@Composable private fun StatusRowV3(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(label, color = CSECOND, modifier = Modifier.weight(1f), fontSize = 10.sp); Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) } }

@Composable private fun AboutV3(phone: Boolean) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { BrandV3(phone); Spacer(Modifier.height(12.dp)); Text("Versión 1.2.0 BETA", fontWeight = FontWeight.Bold); Text("Android TV · Google TV · Android móvil · Kotlin · Media3", color = CSECOND, fontSize = 10.sp); Spacer(Modifier.height(16.dp)); LegalV3() } }
@Composable private fun LegalV3() { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("© epalma", color = CSECOND, fontSize = 12.sp); Text("+504 99461582 - Tegucigalpa - Honduras", color = CSECOND, fontSize = 11.sp) } }

@Composable private fun ErrorBarV3(message: String, close: () -> Unit, modifier: Modifier = Modifier) { Row(modifier.padding(10.dp).background(Color(0xEE351717), RoundedCornerShape(9.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), fontSize = 10.sp); IconButton(close) { Icon(Icons.Default.Close, "Cerrar") } } }

@Composable
private fun FocusCardV3(click: () -> Unit, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }; val focused by interaction.collectIsFocusedAsState(); LaunchedEffect(focused) { if (focused) onFocus() }; val border by animateColorAsState(if (focused) CAC else Color.Transparent, label = "focus-v3")
    Card(modifier.scale(if (focused) 1.02f else 1f).border(if (focused) 2.dp else 0.dp, border, RoundedCornerShape(10.dp)).clickable(interactionSource = interaction, indication = null, onClick = click).focusable(interactionSource = interaction), colors = CardDefaults.cardColors(containerColor = CPANEL), shape = RoundedCornerShape(10.dp)) { content() }
}

private fun cleanNameV3(c: Channel): String = c.name.replace(Regex("\\s*\\((?:[0-9]{3,4}p|HD|FHD|UHD|4K)\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()
private fun qualityV3(name: String): String? = Regex("\\(([^()]*(?:[0-9]{3,4}p|HD|FHD|UHD|4K)[^()]*)\\)\\s*$", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)
private fun formatV3(epoch: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epoch))
