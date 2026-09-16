package com.epalma.tvespanolplus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class PlaylistRepository(private val context: Context) {
    private val store = LocalStore(context)

    suspend fun initialize(): RepositorySnapshot = withContext(Dispatchers.IO) {
        val playlists = store.ensureDefaults()
        val active = playlists.firstOrNull { it.active } ?: playlists.first()
        val cached = loadCachedCatalog(active)
        RepositorySnapshot(
            playlists = playlists,
            active = active,
            channels = cached?.channels.orEmpty(),
            programs = emptyMap(),
            favorites = store.favorites(),
            recents = store.recents(),
            epgUrl = cached?.epgUrl
        )
    }

    /** Refresh only the M3U catalog; XMLTV is intentionally asynchronous. */
    suspend fun refresh(active: PlaylistConfig, previousChannels: List<Channel>): RefreshPayload = withContext(Dispatchers.IO) {
        val request = PlaylistRequestResolver.playlist(active)
        val text = downloadText(request)
        val parsed = M3uParser.parse(text)
        require(PlaybackPolicy.shouldAcceptRefresh(previousChannels.size, parsed.channels.size)) {
            "La actualización devolvió solo ${parsed.channels.size} canales; se conserva la versión anterior"
        }
        store.writeEncrypted(store.playlistCache(active.id), text.toByteArray())

        val oldById = previousChannels.associateBy { it.id }
        val newById = parsed.channels.associateBy { it.id }
        val added = newById.keys.count { it !in oldById }
        val removed = oldById.keys.count { it !in newById }
        val changed = newById.keys.count { id ->
            val a = oldById[id]; val b = newById[id]
            a != null && b != null && (a.sources.map { it.url } != b.sources.map { it.url } || a.name != b.name || a.group != b.group)
        }

        val now = System.currentTimeMillis()
        val playlists = store.ensureDefaults().map { if (it.id == active.id) it.copy(lastUpdatedEpochMs = now) else it }
        store.savePlaylists(playlists)
        val epgRequest = PlaylistRequestResolver.epg(active, parsed.epgUrl)
        RefreshPayload(
            channels = parsed.channels,
            programs = emptyMap(),
            epgUrl = epgRequest?.url,
            playlists = playlists,
            result = RefreshResult(true, if (added + removed + changed == 0) "Lista al día" else "Lista actualizada", added, removed, changed, parsed.channels.size)
        )
    }

    suspend fun loadCachedPrograms(active: PlaylistConfig, channels: List<Channel>): Map<String, List<Program>> = withContext(Dispatchers.IO) {
        val bytes = store.readEncrypted(store.epgCache(active.id)) ?: return@withContext emptyMap()
        if (channels.isEmpty()) return@withContext emptyMap()
        runCatching {
            bytes.inputStream().use { input -> EpgParser.parse(input, channels.mapNotNull { it.tvgId }.toSet()) }
        }.getOrDefault(emptyMap())
    }

    suspend fun refreshEpg(active: PlaylistConfig, epgUrl: String?, channels: List<Channel>): Map<String, List<Program>> = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext emptyMap()
        val request = PlaylistRequestResolver.epg(active, epgUrl) ?: return@withContext emptyMap()
        val bytes = downloadBytes(request, maxBytes = 40 * 1024 * 1024, readTimeoutMs = 20_000)
        store.writeEncrypted(store.epgCache(active.id), bytes)
        bytes.inputStream().use { EpgParser.parse(it, channels.mapNotNull { c -> c.tvgId }.toSet()) }
    }

    suspend fun addPlaylist(
        name: String,
        url: String,
        username: String = "",
        password: String = "",
        authMode: PlaylistAuthMode = PlaylistAuthMode.NONE
    ): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        val candidate = PlaylistConfig(
            id = sha256("${name.trim()}|${System.nanoTime()}").take(16),
            name = name.trim(),
            url = url.trim(),
            username = username.trim(),
            password = password,
            authMode = authMode,
            active = false
        )
        PlaylistRequestResolver.validate(candidate)
        val current = store.ensureDefaults()
        val updated = current + candidate
        store.savePlaylists(updated)
        updated
    }

    suspend fun updatePlaylist(
        id: String,
        name: String,
        url: String,
        username: String,
        password: String,
        authMode: PlaylistAuthMode
    ): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        require(id != LocalStore.DEFAULT_ID) { "La lista predeterminada de TV Español+ está protegida y no se puede modificar" }
        val current = store.ensureDefaults()
        val old = current.firstOrNull { it.id == id } ?: error("Lista no encontrada")
        val candidate = old.copy(
            name = name.trim(),
            url = url.trim(),
            username = username.trim(),
            password = password,
            authMode = authMode
        )
        PlaylistRequestResolver.validate(candidate)
        val updated = current.map { if (it.id == id) candidate else it }
        store.savePlaylists(updated)
        store.deleteSensitiveCaches(id)
        updated
    }

    suspend fun deletePlaylist(id: String): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        require(id != LocalStore.DEFAULT_ID) { "La lista predeterminada de TV Español+ no se puede eliminar" }
        val current = store.ensureDefaults()
        require(current.size > 1) { "Debe existir al menos una lista" }
        val wasActive = current.firstOrNull { it.id == id }?.active == true
        val remaining = current.filterNot { it.id == id }.toMutableList()
        if (wasActive && remaining.none { it.active }) remaining[0] = remaining[0].copy(active = true)
        store.savePlaylists(remaining)
        store.deleteSensitiveCaches(id)
        remaining
    }

    suspend fun setActive(id: String): Pair<List<PlaylistConfig>, RepositorySnapshot> = withContext(Dispatchers.IO) {
        val current = store.ensureDefaults()
        val updated = current.map { it.copy(active = it.id == id) }
        store.savePlaylists(updated)
        val active = updated.first { it.active }
        val cached = loadCachedCatalog(active)
        updated to RepositorySnapshot(updated, active, cached?.channels.orEmpty(), emptyMap(), store.favorites(), store.recents(), cached?.epgUrl)
    }

    suspend fun toggleFavorite(channelId: String): Set<String> = withContext(Dispatchers.IO) {
        val set = store.favorites().toMutableSet()
        if (!set.add(channelId)) set.remove(channelId)
        store.saveFavorites(set); set
    }

    suspend fun recordRecent(channelId: String): List<String> = withContext(Dispatchers.IO) {
        val updated = (listOf(channelId) + store.recents().filterNot { it == channelId }).take(20)
        store.saveRecents(updated); updated
    }

    private fun loadCachedCatalog(active: PlaylistConfig): CachedData? {
        val bytes = store.readEncrypted(store.playlistCache(active.id)) ?: return loadLegacyCachedCatalog(active)
        return parseCached(active, bytes)
    }

    private fun loadLegacyCachedCatalog(active: PlaylistConfig): CachedData? {
        val legacy = context.cacheDir.resolve("playlist_${active.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.m3u")
        if (!legacy.exists()) return null
        val bytes = runCatching { legacy.readBytes() }.getOrNull() ?: return null
        runCatching { store.writeEncrypted(store.playlistCache(active.id), bytes); legacy.delete() }
        return parseCached(active, bytes)
    }

    private fun parseCached(active: PlaylistConfig, bytes: ByteArray): CachedData? = runCatching {
        val parsed = M3uParser.parse(bytes.toString(Charsets.UTF_8))
        val epg = PlaylistRequestResolver.epg(active, parsed.epgUrl)?.url
        CachedData(parsed.channels, emptyMap(), epg)
    }.getOrNull()

    private fun downloadText(request: PlaylistRequest): String =
        downloadBytes(request, 8 * 1024 * 1024, readTimeoutMs = 12_000).toString(Charsets.UTF_8)

    private fun downloadBytes(request: PlaylistRequest, maxBytes: Int, readTimeoutMs: Int): ByteArray {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TV-Espanol-Plus/1.4 Android")
            setRequestProperty("Accept", "*/*")
            request.authorizationHeader?.let { setRequestProperty("Authorization", it) }
        }
        try {
            val code = connection.responseCode
            require(code in 200..299) { "El servidor de la lista respondió HTTP $code" }
            val declared = connection.contentLengthLong
            require(declared <= maxBytes || declared < 0) { "Archivo demasiado grande" }
            return connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(32 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    require(total <= maxBytes) { "Archivo demasiado grande" }
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

data class RepositorySnapshot(
    val playlists: List<PlaylistConfig>,
    val active: PlaylistConfig,
    val channels: List<Channel>,
    val programs: Map<String, List<Program>>,
    val favorites: Set<String>,
    val recents: List<String>,
    val epgUrl: String?
)

data class CachedData(val channels: List<Channel>, val programs: Map<String, List<Program>>, val epgUrl: String?)

data class RefreshPayload(
    val channels: List<Channel>,
    val programs: Map<String, List<Program>>,
    val epgUrl: String?,
    val playlists: List<PlaylistConfig>,
    val result: RefreshResult
)
