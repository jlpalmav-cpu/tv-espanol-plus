package com.epalma.tvespanolplus

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlaylistRepository(app)
    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()
    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()
    private val _filter = MutableStateFlow("TV general")
    val filter: StateFlow<String> = _filter.asStateFlow()
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
            val snapshot = repo.initialize()
            // Never trap the user behind a full-screen "preparing" state. The
            // cached catalog (if any) becomes usable immediately and refresh runs
            // independently with a hard timeout.
            _ui.update {
                it.copy(
                    loading = false,
                    statusMessage = if (snapshot.channels.isEmpty()) "Preparando canales en segundo plano…" else "Listo",
                    playlists = snapshot.playlists,
                    activePlaylist = snapshot.active,
                    channels = snapshot.channels,
                    programs = emptyMap(),
                    favorites = snapshot.favorites,
                    recentIds = snapshot.recents
                )
            }
            if (snapshot.channels.isNotEmpty()) syncEpg(snapshot.active, snapshot.epgUrl, snapshot.channels)
            refresh(silent = snapshot.channels.isNotEmpty())
        }
    }

    fun go(screen: Screen) { _screen.value = screen }
    fun openCategory(name: String) { _filter.value = name; _screen.value = Screen.CHANNELS }

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
            delay(240)
            val state = _ui.value
            val results = SearchEngine.search(query, state.channels, state.programs)
            _ui.update { it.copy(searchResults = results) }
        }
    }

    fun openSearch() { _screen.value = Screen.SEARCH }

    fun play(channel: Channel) {
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
        _ui.update { it.copy(dualLeft = base, dualRight = null, dualAudioSide = DualSide.LEFT) }
        _screen.value = Screen.DUAL
    }

    fun setDualChannel(side: DualSide, channel: Channel) {
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

    // Backward-compatible entry points used by older composables; secure V5 UI uses the methods above.
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
