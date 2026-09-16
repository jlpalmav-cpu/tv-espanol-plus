package com.epalma.tvespanolplus

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val V4BG = Color(0xFF101114)
private val V4PANEL = Color(0xFF191B20)
private val V4ACCENT = Color(0xFF19B5A5)
private val V4SECOND = Color(0xFFB8BBC3)
private val V4LIVE = Color(0xFFE53935)

@Composable
fun TVEspanolPlusRootV4(vm: MainViewModel, onExit: () -> Unit) {
    val screen by vm.screen.collectAsState()
    val ui by vm.ui.collectAsState()
    val filter by vm.filter.collectAsState()
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val phone = !remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val portrait = config.screenHeightDp >= config.screenWidthDp

    when (screen) {
        Screen.CHANNELS -> ChannelsRootV4(ui, filter, vm, phone, onExit)
        Screen.PLAYER -> PlayerRootV4(ui, vm, phone, portrait)
        else -> TVEspanolPlusRootV3(vm, onExit)
    }
}

@Composable
private fun ChannelsRootV4(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean, onExit: () -> Unit) {
    var confirmExit by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = V4BG) {
            Column(Modifier.fillMaxSize()) {
                HeaderV4(phone, ui.refreshing, vm, { confirmExit = true })
                Box(Modifier.weight(1f)) { ChannelNavigationV4(ui, filter, vm, phone) }
                if (phone) BottomV4(vm)
            }
        }
    }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("¿Salir de TV Español+?") },
        text = { Text("Se cerrará la aplicación y se liberarán los reproductores.") },
        confirmButton = { TextButton(onClick = onExit) { Text("Salir") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun HeaderV4(phone: Boolean, refreshing: Boolean, vm: MainViewModel, exit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(if (phone) 54.dp else 72.dp).background(V4PANEL).padding(horizontal = if (phone) 6.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.clickable { vm.go(Screen.HOME) }, verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 32.dp else 48.dp))
            Spacer(Modifier.width(4.dp)); Text("TV Español", fontSize = if (phone) 16.sp else 26.sp, fontWeight = FontWeight.Black); Text("+", color = V4ACCENT, fontSize = if (phone) 19.sp else 30.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        if (phone) {
            TopIconV4(Icons.Default.Search, "Buscar") { vm.openSearch() }
            TopIconV4(Icons.Default.Refresh, "Actualizar", !refreshing) { vm.refresh(false) }
            TopIconV4(Icons.Default.Settings, "Configuración") { vm.go(Screen.SETTINGS) }
            TopIconV4(Icons.Default.ExitToApp, "Salir", true, exit)
        } else {
            HeaderButtonV4("Buscar", Icons.Default.Search) { vm.openSearch() }
            HeaderButtonV4(if (refreshing) "Actualizando" else "Actualizar", Icons.Default.Refresh, !refreshing) { vm.refresh(false) }
            HeaderButtonV4("Configuración", Icons.Default.Settings) { vm.go(Screen.SETTINGS) }
            HeaderButtonV4("Salir", Icons.Default.ExitToApp, true, exit)
        }
    }
}

@Composable private fun TopIconV4(icon: ImageVector, desc: String, enabled: Boolean = true, action: () -> Unit) { IconButton(action, enabled = enabled, modifier = Modifier.size(36.dp)) { Icon(icon, desc, Modifier.size(21.dp)) } }
@Composable private fun HeaderButtonV4(text: String, icon: ImageVector, enabled: Boolean = true, action: () -> Unit) { OutlinedButton(action, enabled = enabled, modifier = Modifier.padding(start = 6.dp)) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text(text) } }

@Composable
private fun BottomV4(vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(V4PANEL), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        TextButton({ vm.go(Screen.HOME) }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Home, "Inicio", tint = V4SECOND); Text("Inicio", color = V4SECOND, fontSize = 9.sp) } }
        TextButton({ vm.openSearch() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Search, "Buscar", tint = V4SECOND); Text("Buscar", color = V4SECOND, fontSize = 9.sp) } }
        TextButton({ vm.openCategory("Favoritos") }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Favorite, "Favoritos", tint = V4SECOND); Text("Favoritos", color = V4SECOND, fontSize = 9.sp) } }
        TextButton({ vm.go(Screen.SETTINGS) }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Settings, "Más", tint = V4SECOND); Text("Más", color = V4SECOND, fontSize = 9.sp) } }
    }
}

@Composable
private fun ChannelNavigationV4(ui: AppUiState, filter: String, vm: MainViewModel, phone: Boolean) {
    val normalized = TextNormalizer.normalize(filter)
    when {
        normalized == "paises" -> CountryFoldersV4(ui, vm, phone)
        filter.startsWith("País:") -> ChannelGridV4(filter.removePrefix("País:"), ChannelClassifier.channelsForCountry(ui.channels, filter.removePrefix("País:")), ui, vm, phone)
        filter.startsWith("Subcat:") -> {
            val payload = filter.removePrefix("Subcat:")
            val category = payload.substringBefore('|')
            val sub = payload.substringAfter('|', "")
            ChannelGridV4("$category · $sub", ChannelClassifier.channelsForSubcategory(ui.channels, category, sub), ui, vm, phone)
        }
        normalized == "favoritos" -> ChannelGridV4("Favoritos", ui.channels.distinctBy { it.id }.filter { it.id in ui.favorites }, ui, vm, phone)
        else -> {
            val subcategories = ChannelClassifier.subcategoriesForCategory(ui.channels, filter)
            if (subcategories.isNotEmpty()) SubcategoryFoldersV4(filter, subcategories, vm, phone)
            else ChannelGridV4(filter, ChannelClassifier.channelsForCategory(ui.channels, filter), ui, vm, phone)
        }
    }
}

@Composable
private fun SubcategoryFoldersV4(category: String, groups: List<SubcategoryGroup>, vm: MainViewModel, phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(category, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Subcategorías para encontrar canales más rápido", color = V4SECOND, fontSize = 11.sp) }
            Text("${groups.sumOf { it.count }} canales", color = V4SECOND, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(groups, key = { it.name }) { group -> FolderCardV4(group.name, group.count, Icons.Default.Folder) { vm.openCategory("Subcat:$category|${group.name}") } }
        }
    }
}

@Composable
private fun CountryFoldersV4(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    val countries = remember(ui.channels) { ChannelClassifier.countries(ui.channels) }
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Países", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Una carpeta por país", color = V4SECOND, fontSize = 11.sp) }
            Text("${countries.size} países", color = V4SECOND, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(countries, key = { it.name }) { country -> FolderCardV4(country.name, country.count, Icons.Default.Public) { vm.openCategory("País:${country.name}") } }
        }
    }
}

@Composable
private fun FolderCardV4(name: String, count: Int, icon: ImageVector, click: () -> Unit) {
    Card(onClick = click, modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = V4PANEL), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = V4ACCENT, modifier = Modifier.size(27.dp)); Spacer(Modifier.width(7.dp)); Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.height(5.dp)); Text("$count canales", color = V4SECOND, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ChannelGridV4(title: String, list: List<Channel>, ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${list.size} canales", color = V4SECOND, fontSize = 11.sp) }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Fixed(if (phone) 2 else 4), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list.distinctBy { it.id }, key = { it.id }) { c ->
                Card(onClick = { vm.play(c) }, modifier = Modifier.fillMaxWidth().height(if (phone) 116.dp else 106.dp), colors = CardDefaults.cardColors(containerColor = V4PANEL), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(if (phone) 42.dp else 48.dp), contentScale = ContentScale.Fit) else Icon(Icons.Default.Tv, null, tint = V4SECOND, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.width(7.dp)); Text(cleanNameV4(c), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = if (phone) 12.sp else 14.sp)
                            IconButton({ vm.toggleFavorite(c.id) }, modifier = Modifier.size(32.dp)) { Icon(if (c.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (c.id in ui.favorites) V4LIVE else V4SECOND, modifier = Modifier.size(18.dp)) }
                        }
                        Spacer(Modifier.height(4.dp)); Text(ChannelClassifier.countryName(c), color = V4SECOND, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRootV4(ui: AppUiState, vm: MainViewModel, phone: Boolean, portrait: Boolean) {
    val channel = ui.selectedChannel ?: return
    var error by remember(channel.id) { mutableStateOf<String?>(null) }
    var showTracks by remember(channel.id) { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = Color.Black) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ResilientPlayer(
                channel = channel,
                modifier = Modifier.fillMaxSize(),
                volume = 1f,
                controls = true,
                showTrackMenu = showTracks,
                onTrackMenuDismiss = { showTracks = false },
                onTerminalError = { error = it }
            )
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.78f)).padding(7.dp)) {
                Text(cleanNameV4(channel), color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (phone) 14.sp else 16.sp, maxLines = 1)
                Text("${ChannelClassifier.categoryFor(channel)} · ${ChannelClassifier.countryName(channel)}", color = V4SECOND, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton({ vm.toggleFavorite(channel.id) }, Modifier.weight(1f)) { Icon(if (channel.id in ui.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(17.dp)); if (!phone || !portrait) Text(" Favorito") }
                    OutlinedButton({ showTracks = true }, Modifier.weight(1f)) { Icon(Icons.Default.Subtitles, null, Modifier.size(17.dp)); Text(if (phone && portrait) " Idioma" else " Audio/Sub") }
                    OutlinedButton({ vm.startDual(channel) }, Modifier.weight(1f)) { Text(if (phone && portrait) "▣ Dual" else "▣ Vista doble") }
                    OutlinedButton({ vm.go(Screen.HOME) }, Modifier.weight(1f)) { Text("Volver") }
                }
            }
            error?.let { Text(it, Modifier.align(Alignment.Center).background(Color.Black.copy(.85f), RoundedCornerShape(8.dp)).padding(14.dp), color = Color.White) }
        }
    }
}

private fun cleanNameV4(c: Channel): String = c.name.replace(Regex("\\s*\\((?:[0-9]{3,4}p|HD|FHD|UHD|4K)\\)\\s*$", RegexOption.IGNORE_CASE), "").trim()
