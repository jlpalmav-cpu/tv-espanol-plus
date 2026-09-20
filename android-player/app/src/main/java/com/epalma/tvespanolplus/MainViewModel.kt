package com.epalma.tvespanolplus

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlaylistRepository(app)
    private val preferenceStore = AppPreferenceStore(app)

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _filter = MutableStateFlow("TV general")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _preferences = MutableStateFlow(preferenceStore.load())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _parentalGate = MutableStateFlow<ParentalGate?>(null)
    val parentalGate: StateFlow<ParentalGate?> = _parentalGate.asStateFlow()

    private val _parentalUnlocked = MutableStateFlow(false)
    val parentalUnlocked: StateFlow<Boolean> = _parentalUnlocked.asStateFlow()

    private var searchJob: Job? = null
    private var epgJob: Job? = null
    private var refreshJob: Job? = null
    private var everConnected = false

    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (everConnected) refresh(silent = true) else everConnected = true
        }
    }

    init {
        runCatching { connectivity.registerDefaultNetworkCallback(networkCallback) }
        viewModelScope.launch {
            runCatching { repo.initialize() }
                .onSuccess { snapshot ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            statusMessage = if (snapshot.channels.isEmpty()) "Preparando canales en segundo plano…" else "Listo",
                            playlists = snapshot.playlists,
                            activePlaylist = snapshot.active,
                            channels = snapshot.channels,
                            programs = emptyMap(),
                            favorites = snapshot.favorites,
                            recentIds = snapshot.recents,
                            error = null
                        )
                    }
                    if (snapshot.channels.isNotEmpty()) syncEpg(snapshot.active, snapshot.epgUrl, snapshot.channels)
                    refresh(silent = snapshot.channels.isNotEmpty())
                }
                .onFailure { error ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            statusMessage = "La app inició en modo recuperación",
                            error = "No se pudo preparar el catálogo local: ${error.message ?: "error desconocido"}"
                        )
                    }
                }
        }
    }

    fun go(screen: Screen) { _screen.value = screen }

    fun openCategory(name: String) {
        val prefs = _preferences.value
        if (!_parentalUnlocked.value && ParentalPolicy.isRestrictedCategory(name, prefs)) {
            _parentalGate.value = ParentalGate(
                title = "Contenido protegido",
                message = "Ingresa el PIN parental para abrir esta sección.",
                pendingCategory = name
            )
            return
        }
        openCategoryUnlocked(name)
    }

    private fun openCategoryUnlocked(name: String) {
        _filter.value = name
        _screen.value = Screen.CHANNELS
    }

    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        val active = _ui.value.activePlaylist ?: return
        refreshJob = viewModelScope.launch {
            _ui.update {
                it.copy(
                    refreshing = true,
                    error = null,
                    statusMessage = if (silent) it.statusMessage else "Actualizando canales en segundo plano…"
                )
            }
            try {
                val payload = withTimeout(15_000) { repo.refresh(active, _ui.value.channels) }
                val resolvedActive = payload.playlists.firstOrNull { p -> p.active } ?: active
                _ui.update {
                    it.copy(
                        loading = false,
                        statusMessage = payload.result.message,
                        channels = payload.channels,
                        playlists = payload.playlists,
                        activePlaylist = resolvedActive,
                        lastRefresh = payload.result,
                        error = null
                    )
                }
                syncEpg(resolvedActive, payload.epgUrl, payload.channels)
                if (_ui.value.searchQuery.isNotBlank()) search(_ui.value.searchQuery)
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        loading = false,
                        statusMessage = if (it.channels.isEmpty()) "Sin catálogo todavía · puedes reintentar" else "Usando la última lista disponible",
                        error = when (t) {
                            is kotlinx.coroutines.TimeoutCancellationException -> "La actualización tardó demasiado. Se conservó la última lista disponible."
                            else -> t.message ?: "No se pudo actualizar"
                        }
                    )
                }
            } finally {
                _ui.update { it.copy(refreshing = false) }
            }
        }
    }

    private fun syncEpg(active: PlaylistConfig, epgUrl: String?, channels: List<Channel>) {
        epgJob?.cancel()
        epgJob = viewModelScope.launch {
            val playlistId = active.id
            val cached = runCatching { repo.loadCachedPrograms(active, channels) }.getOrDefault(emptyMap())
            if (cached.isNotEmpty() && _ui.value.activePlaylist?.id == playlistId) {
                _ui.update { it.copy(programs = cached) }
                if (_ui.value.searchQuery.isNotBlank()) search(_ui.value.searchQuery)
            }
            val fresh = runCatching { withTimeout(35_000) { repo.refreshEpg(active, epgUrl, channels) } }.getOrDefault(emptyMap())
            if (fresh.isNotEmpty() && _ui.value.activePlaylist?.id == playlistId) {
                _ui.update { it.copy(programs = fresh) }
                if (_ui.value.searchQuery.isNotBlank()) search(_ui.value.searchQuery)
            }
        }
    }

    fun search(query: String) {
        _ui.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(170)
            val state = _ui.value
            val results = withContext(Dispatchers.Default) {
                SearchEngine.search(query, state.channels, state.programs)
            }
            if (_ui.value.searchQuery == query) _ui.update { it.copy(searchResults = results) }
        }
    }

    fun openSearch() { _screen.value = Screen.SEARCH }

    fun play(channel: Channel) {
        val prefs = _preferences.value
        if (!_parentalUnlocked.value && ParentalPolicy.isRestrictedChannel(channel, prefs)) {
            _parentalGate.value = ParentalGate(
                title = "Canal protegido",
                message = "Ingresa el PIN parental para reproducir ${channel.name}.",
                pendingChannel = channel
            )
            return
        }
        playUnlocked(channel)
    }

    private fun playUnlocked(channel: Channel) {
        _ui.update { it.copy(selectedChannel = channel) }
        _screen.value = Screen.PLAYER
        viewModelScope.launch {
            val recent = repo.recordRecent(channel.id)
            _ui.update { it.copy(recentIds = recent) }
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            val fav = repo.toggleFavorite(channelId)
            _ui.update { it.copy(favorites = fav) }
        }
    }

    fun startDual(base: Channel) {
        val prefs = _preferences.value
        if (!_parentalUnlocked.value && ParentalPolicy.isRestrictedChannel(base, prefs)) {
            _parentalGate.value = ParentalGate(
                title = "Canal protegido",
                message = "Ingresa el PIN parental antes de usar Vista doble.",
                pendingChannel = base,
                pendingStartDual = true
            )
            return
        }
        startDualUnlocked(base)
    }

    private fun startDualUnlocked(base: Channel) {
        _ui.update { it.copy(dualLeft = base, dualRight = null, dualAudioSide = DualSide.LEFT) }
        _screen.value = Screen.DUAL
    }

    fun setDualChannel(side: DualSide, channel: Channel) {
        val prefs = _preferences.value
        if (!_parentalUnlocked.value && ParentalPolicy.isRestrictedChannel(channel, prefs)) {
            _parentalGate.value = ParentalGate(
                title = "Canal protegido",
                message = "Ingresa el PIN parental para agregar este canal a Vista doble.",
                pendingChannel = channel,
                pendingDualSide = side
            )
            return
        }
        setDualChannelUnlocked(side, channel)
    }

    private fun setDualChannelUnlocked(side: DualSide, channel: Channel) {
        _ui.update { if (side == DualSide.LEFT) it.copy(dualLeft = channel) else it.copy(dualRight = channel) }
        viewModelScope.launch { repo.recordRecent(channel.id) }
    }

    fun setDualAudio(side: DualSide) { _ui.update { it.copy(dualAudioSide = side) } }
    fun swapDual() { _ui.update { it.copy(dualLeft = it.dualRight, dualRight = it.dualLeft, dualAudioSide = if (it.dualAudioSide == DualSide.LEFT) DualSide.RIGHT else DualSide.LEFT) } }
    fun fullScreenFromDual(side: DualSide) {
        val channel = if (side == DualSide.LEFT) _ui.value.dualLeft else _ui.value.dualRight
        if (channel != null) play(channel)
    }
    fun closeDualSide(side: DualSide) {
        val remaining = if (side == DualSide.LEFT) _ui.value.dualRight else _ui.value.dualLeft
        if (remaining != null) play(remaining) else go(Screen.HOME)
    }

    fun setUiTextScale(scale: Float) = updatePreferences { it.copy(uiTextScale = scale.coerceIn(0.85f, 1.35f)) }
    fun setPreferredAudioLanguage(language: String) = updatePreferences { it.copy(preferredAudioLanguage = language) }
    fun setPreferredSubtitleLanguage(language: String) = updatePreferences { it.copy(preferredSubtitleLanguage = language) }
    fun setSubtitleTextSize(sizeSp: Float) = updatePreferences { it.copy(subtitleTextSizeSp = sizeSp.coerceIn(16f, 30f)) }

    fun setParentalPin(pin: String): Boolean {
        if (!ParentalPolicy.validPinFormat(pin)) {
            _ui.update { it.copy(error = "El PIN parental debe tener de 4 a 6 dígitos.") }
            return false
        }
        updatePreferences { it.copy(parentalEnabled = true, parentalPinHash = AppPreferenceStore.hashPin(pin)) }
        _parentalUnlocked.value = true
        _ui.update { it.copy(error = null) }
        return true
    }

    fun setParentalEnabled(enabled: Boolean) {
        val current = _preferences.value
        if (enabled && current.parentalPinHash.isBlank()) {
            _ui.update { it.copy(error = "Primero configura un PIN parental.") }
            return
        }
        if (!enabled && current.parentalEnabled && !_parentalUnlocked.value) {
            _parentalGate.value = ParentalGate(
                title = "Desbloquear control parental",
                message = "Ingresa el PIN antes de desactivar la protección."
            )
            return
        }
        updatePreferences { it.copy(parentalEnabled = enabled) }
    }

    fun toggleChannelLock(channelId: String) {
        val current = _preferences.value
        if (current.parentalEnabled && !_parentalUnlocked.value) {
            _parentalGate.value = ParentalGate(
                title = "Desbloquear control parental",
                message = "Ingresa el PIN antes de cambiar canales protegidos."
            )
            return
        }
        updatePreferences {
            val next = it.lockedChannelIds.toMutableSet()
            if (!next.add(channelId)) next.remove(channelId)
            it.copy(lockedChannelIds = next)
        }
    }

    fun requestParentalUnlock() {
        val current = _preferences.value
        if (!current.parentalEnabled || current.parentalPinHash.isBlank()) {
            _parentalUnlocked.value = true
            return
        }
        _parentalGate.value = ParentalGate(
            title = "Desbloquear control parental",
            message = "Ingresa el PIN para administrar la protección."
        )
    }

    fun submitParentalPin(pin: String): Boolean {
        val current = _preferences.value
        if (current.parentalPinHash.isBlank() || AppPreferenceStore.hashPin(pin) != current.parentalPinHash) {
            _ui.update { it.copy(error = "PIN parental incorrecto.") }
            return false
        }
        _parentalUnlocked.value = true
        val gate = _parentalGate.value
        _parentalGate.value = null
        _ui.update { it.copy(error = null) }

        when {
            gate?.pendingCategory != null -> openCategoryUnlocked(gate.pendingCategory)
            gate?.pendingChannel != null && gate.pendingStartDual -> startDualUnlocked(gate.pendingChannel)
            gate?.pendingChannel != null && gate.pendingDualSide != null -> setDualChannelUnlocked(gate.pendingDualSide, gate.pendingChannel)
            gate?.pendingChannel != null -> playUnlocked(gate.pendingChannel)
        }
        return true
    }

    fun dismissParentalGate() { _parentalGate.value = null }

    private fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_preferences.value)
        _preferences.value = next
        runCatching { preferenceStore.save(next) }
    }

    fun addPlaylistSecure(name: String, url: String, username: String, password: String, authMode: PlaylistAuthMode) = viewModelScope.launch {
        runCatching { repo.addPlaylist(name, url, username, password, authMode) }
            .onSuccess { list -> _ui.update { it.copy(playlists = list, error = null) } }
            .onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun editPlaylistSecure(id: String, name: String, url: String, username: String, password: String, authMode: PlaylistAuthMode) = viewModelScope.launch {
        runCatching { repo.updatePlaylist(id, name, url, username, password, authMode) }
            .onSuccess { list -> _ui.update { it.copy(playlists = list, activePlaylist = list.firstOrNull { p -> p.active }, error = null) } }
            .onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun addPlaylist(name: String, url: String) = addPlaylistSecure(name, url, "", "", PlaylistAuthMode.NONE)
    fun editPlaylist(id: String, name: String, url: String) {
        val current = _ui.value.playlists.firstOrNull { it.id == id } ?: return
        editPlaylistSecure(id, name, url, current.username, current.password, current.authMode)
    }

    fun deletePlaylist(id: String) = viewModelScope.launch {
        runCatching { repo.deletePlaylist(id) }
            .onSuccess { list ->
                val active = list.first { p -> p.active }
                val (_, snap) = repo.setActive(active.id)
                _ui.update { it.copy(playlists = list, activePlaylist = active, channels = snap.channels, programs = emptyMap(), error = null) }
                syncEpg(active, snap.epgUrl, snap.channels)
            }
            .onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun activatePlaylist(id: String) = viewModelScope.launch {
        runCatching { repo.setActive(id) }
            .onSuccess { (list, snap) ->
                _ui.update { it.copy(playlists = list, activePlaylist = snap.active, channels = snap.channels, programs = emptyMap(), error = null) }
                if (snap.channels.isNotEmpty()) syncEpg(snap.active, snap.epgUrl, snap.channels)
                refresh(silent = true)
            }
            .onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun clearError() { _ui.update { it.copy(error = null) } }

    override fun onCleared() {
        searchJob?.cancel()
        epgJob?.cancel()
        refreshJob?.cancel()
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
    }
}
