package com.epalma.tvespanolplus

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val V5BG = Color(0xFF101114)
private val V5PANEL = Color(0xFF191B20)
private val V5ACCENT = Color(0xFF19B5A5)
private val V5SECOND = Color(0xFFB8BBC3)
private val V5LIVE = Color(0xFFE53935)

@Composable
fun TVEspanolPlusRootV5(vm: MainViewModel, onExit: () -> Unit) {
    val screen by vm.screen.collectAsState()
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val phone = !remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    when (screen) {
        Screen.PLAYLISTS -> SecurePlaylistsV5(ui, vm, phone)
        Screen.SEARCH -> QuerySearchV5(ui, vm, phone)
        else -> TVEspanolPlusRootV4(vm, onExit)
    }
}

@Composable
private fun SimpleShellV5(phone: Boolean, vm: MainViewModel, title: String, content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), color = V5BG) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(if (phone) 54.dp else 68.dp).background(V5PANEL).padding(horizontal = if (phone) 7.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.clickable { vm.go(Screen.HOME) }, verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_tv_espanol), "TV Español+", Modifier.size(if (phone) 32.dp else 46.dp))
                        Spacer(Modifier.width(5.dp)); Text("TV Español+", fontWeight = FontWeight.Black, fontSize = if (phone) 16.sp else 24.sp)
                    }
                    Spacer(Modifier.weight(1f)); Text(title, color = V5SECOND, fontSize = if (phone) 11.sp else 14.sp, maxLines = 1)
                }
                Box(Modifier.weight(1f)) { content() }
                if (phone) {
                    Row(Modifier.fillMaxWidth().height(52.dp).background(V5PANEL), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ vm.go(Screen.HOME) }) { Icon(Icons.Default.Home, "Inicio", tint = V5SECOND); Spacer(Modifier.width(3.dp)); Text("Inicio", color = V5SECOND, fontSize = 10.sp) }
                        TextButton({ vm.openSearch() }) { Icon(Icons.Default.Search, "Buscar", tint = V5ACCENT); Spacer(Modifier.width(3.dp)); Text("Buscar", color = V5ACCENT, fontSize = 10.sp) }
                        TextButton({ vm.openCategory("Favoritos") }) { Icon(Icons.Default.Favorite, "Favoritos", tint = V5SECOND); Spacer(Modifier.width(3.dp)); Text("Favoritos", color = V5SECOND, fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurePlaylistsV5(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    var editId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PlaylistAuthMode.NONE) }

    fun reset() {
        editId = null; name = ""; url = ""; username = ""; password = ""; mode = PlaylistAuthMode.NONE
    }

    SimpleShellV5(phone, vm, "Mis listas") {
        LazyColumn(Modifier.fillMaxSize().padding(if (phone) 10.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Mis listas", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("URL y contraseña se guardan cifradas y nunca se muestran en pantalla.", color = V5SECOND, fontSize = 10.sp)
            }
            items(ui.playlists, key = { it.id }) { p ->
                val protected = p.id == LocalStore.DEFAULT_ID
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = V5PANEL),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (protected) Icons.Default.Lock else Icons.Default.Security, null, tint = V5ACCENT)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text((if (p.active) "✓ " else "") + p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(PlaylistRequestResolver.safeDescription(p), color = V5SECOND, fontSize = 9.sp)
                            Text(authLabelV5(p.authMode) + if (protected) " · Predeterminada protegida" else "", color = V5SECOND, fontSize = 9.sp)
                        }
                        if (!p.active) TextButton({ vm.activatePlaylist(p.id) }) { Text("Usar") }
                        if (!protected) TextButton({
                            editId = p.id
                            name = p.name
                            url = ""
                            username = if (p.authMode == PlaylistAuthMode.NONE) "" else p.username
                            password = ""
                            mode = p.authMode
                        }) { Text("Editar") }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(if (editId == null) "Agregar lista" else "Editar lista segura", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                if (editId != null) Text("Deja URL o contraseña en blanco para conservar el valor cifrado actual.", color = V5SECOND, fontSize = 9.sp)
            }
            item {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre de la lista") }, singleLine = true)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuthModeButtonV5("M3U URL", mode == PlaylistAuthMode.NONE, Modifier.weight(1f)) { mode = PlaylistAuthMode.NONE }
                    AuthModeButtonV5("Usuario", mode == PlaylistAuthMode.BASIC, Modifier.weight(1f)) { mode = PlaylistAuthMode.BASIC }
                    AuthModeButtonV5("Xtream", mode == PlaylistAuthMode.XTREAM, Modifier.weight(1f)) { mode = PlaylistAuthMode.XTREAM }
                }
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (mode == PlaylistAuthMode.XTREAM) "URL base protegida" else "URL protegida") },
                    placeholder = { Text(if (editId == null) "Se ocultará mientras escribes" else "•••••• (conservar si queda vacío)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    singleLine = true
                )
            }
            if (mode != PlaylistAuthMode.NONE) {
                item { OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Usuario") }, singleLine = true) }
                item {
                    OutlinedTextField(
                        password,
                        { password = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Contraseña protegida") },
                        placeholder = { Text(if (editId == null) "••••••" else "•••••• (conservar si queda vacío)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
                item { Text("Por seguridad, las listas con credenciales requieren HTTPS.", color = V5SECOND, fontSize = 9.sp) }
            }
            item {
                val old = editId?.let { id -> ui.playlists.firstOrNull { it.id == id } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(onClick = {
                        if (editId == null) {
                            vm.addPlaylistSecure(name, url, username, password, mode)
                        } else if (old != null) {
                            val finalUrl = url.ifBlank { old.url }
                            val finalUser = if (mode == PlaylistAuthMode.NONE) "" else username.ifBlank { old.username }
                            val finalPassword = if (mode == PlaylistAuthMode.NONE) "" else password.ifBlank { old.password }
                            vm.editPlaylistSecure(old.id, name.ifBlank { old.name }, finalUrl, finalUser, finalPassword, mode)
                        }
                        reset()
                    }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                    if (editId != null && old != null) {
                        OutlinedButton({ vm.deletePlaylist(old.id); reset() }, Modifier.weight(1f)) { Text("Eliminar") }
                        OutlinedButton({ reset() }, Modifier.weight(1f)) { Text("Cancelar") }
                    }
                }
            }
            item {
                OutlinedButton({ vm.refresh(false) }, Modifier.fillMaxWidth(), enabled = !ui.refreshing) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(5.dp)); Text(if (ui.refreshing) "Actualizando en segundo plano…" else "Actualizar lista activa")
                }
            }
        }
    }
}

@Composable
private fun AuthModeButtonV5(text: String, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    if (selected) Button(click, modifier) { Text(text, fontSize = 10.sp) }
    else OutlinedButton(click, modifier) { Text(text, fontSize = 10.sp) }
}

private fun authLabelV5(mode: PlaylistAuthMode): String = when (mode) {
    PlaylistAuthMode.NONE -> "M3U por URL"
    PlaylistAuthMode.BASIC -> "Usuario/contraseña · autenticación básica"
    PlaylistAuthMode.XTREAM -> "Xtream · usuario/contraseña"
}

@Composable
private fun QuerySearchV5(ui: AppUiState, vm: MainViewModel, phone: Boolean) {
    SimpleShellV5(phone, vm, "Súper Búsqueda") {
        Column(Modifier.fillMaxSize().padding(if (phone) 10.dp else 16.dp)) {
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = vm::search,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (ui.searchQuery.isNotBlank()) IconButton({ vm.search("") }) { Icon(Icons.Default.Close, "Limpiar") } },
                label = { Text("Súper Búsqueda") },
                placeholder = { Text("fc barcelos, noticias Honduras, Fórmula 1") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true
            )
            Spacer(Modifier.height(7.dp))
            if (ui.searchQuery.length < 2) {
                Text("Escribe al menos 2 caracteres. Se respetan todos los términos de la consulta y luego se prioriza EN VIVO, próximo, hoy y mañana.", color = V5SECOND, fontSize = 10.sp)
            } else {
                Text("Resultados para “${ui.searchQuery}” · ${ui.searchResults.size}", color = V5SECOND, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                if (ui.searchResults.isEmpty()) {
                    Text("No encontramos contenido que corresponda suficientemente con esa consulta.", color = V5SECOND)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ui.searchResults, key = { "${it.channel.id}:${it.program?.startEpochMs ?: 0}" }) { hit ->
                            Card(onClick = { vm.play(hit.channel) }, colors = CardDefaults.cardColors(containerColor = V5PANEL), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    hit.channel.logo?.let { AsyncImage(it, null, Modifier.size(if (phone) 42.dp else 50.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(8.dp)) }
                                    Column(Modifier.weight(1f)) {
                                        Text(hit.program?.title ?: hit.channel.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${hit.channel.name} · ${ChannelClassifier.categoryFor(hit.channel)} · ${ChannelClassifier.countryName(hit.channel)}", color = V5SECOND, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (hit.temporalBucket == TemporalBucket.LIVE_NOW) "● EN VIVO" else hit.reason.uppercase(), color = if (hit.temporalBucket == TemporalBucket.LIVE_NOW) V5LIVE else V5ACCENT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        TextButton({ vm.startDual(hit.channel) }) { Text("▣ Dual", fontSize = 9.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
