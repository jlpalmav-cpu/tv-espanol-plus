package com.epalma.tvespanolplus

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

    /**
     * Refreshes only the M3U catalog. EPG download/parsing is intentionally
     * excluded so the Home screen never waits for a large XMLTV file.
     */
    suspend fun refresh(active: PlaylistConfig, previousChannels: List<Channel>): RefreshPayload = withContext(Dispatchers.IO) {
        val text = downloadText(active.url)
        val parsed = M3uParser.parse(text)
        require(PlaybackPolicy.shouldAcceptRefresh(previousChannels.size, parsed.channels.size)) {
            "La actualización devolvió solo ${parsed.channels.size} canales; se conserva la versión anterior"
        }
        atomicWrite(store.playlistCache(active.id), text.toByteArray())

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
        RefreshPayload(
            channels = parsed.channels,
            programs = emptyMap(),
            epgUrl = parsed.epgUrl,
            playlists = playlists,
            result = RefreshResult(true, if (added + removed + changed == 0) "Lista al día" else "Lista actualizada", added, removed, changed, parsed.channels.size)
        )
    }

    suspend fun loadCachedPrograms(active: PlaylistConfig, channels: List<Channel>): Map<String, List<Program>> = withContext(Dispatchers.IO) {
        val file = store.epgCache(active.id)
        if (!file.exists() || channels.isEmpty()) return@withContext emptyMap()
        runCatching {
            file.inputStream().use { input -> EpgParser.parse(input, channels.mapNotNull { it.tvgId }.toSet()) }
        }.getOrDefault(emptyMap())
    }

    suspend fun refreshEpg(active: PlaylistConfig, epgUrl: String?, channels: List<Channel>): Map<String, List<Program>> = withContext(Dispatchers.IO) {
        if (epgUrl.isNullOrBlank() || !epgUrl.startsWith("https://", true) || channels.isEmpty()) return@withContext emptyMap()
        val bytes = downloadBytes(epgUrl, maxBytes = 40 * 1024 * 1024, readTimeoutMs = 20_000)
        atomicWrite(store.epgCache(active.id), bytes)
        bytes.inputStream().use { EpgParser.parse(it, channels.mapNotNull { c -> c.tvgId }.toSet()) }
    }

    suspend fun addPlaylist(name: String, url: String): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        require(name.trim().isNotEmpty()) { "Nombre requerido" }
        require(url.startsWith("https://", ignoreCase = true)) { "La URL debe usar HTTPS" }
        val current = store.ensureDefaults()
        val id = sha256("${name.trim()}|${url.trim()}|${System.nanoTime()}").take(16)
        val updated = current + PlaylistConfig(id, name.trim(), url.trim(), active = false)
        store.savePlaylists(updated)
        updated
    }

    suspend fun updatePlaylist(id: String, name: String, url: String): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        require(id != LocalStore.DEFAULT_ID) { "La lista predeterminada de TV Español+ está protegida y no se puede modificar" }
        require(name.trim().isNotEmpty()) { "Nombre requerido" }
        require(url.startsWith("https://", ignoreCase = true)) { "La URL debe usar HTTPS" }
        val updated = store.ensureDefaults().map { if (it.id == id) it.copy(name = name.trim(), url = url.trim()) else it }
        store.savePlaylists(updated); updated
    }

    suspend fun deletePlaylist(id: String): List<PlaylistConfig> = withContext(Dispatchers.IO) {
        require(id != LocalStore.DEFAULT_ID) { "La lista predeterminada de TV Español+ no se puede eliminar" }
        val current = store.ensureDefaults()
        require(current.size > 1) { "Debe existir al menos una lista" }
        val wasActive = current.firstOrNull { it.id == id }?.active == true
        val remaining = current.filterNot { it.id == id }.toMutableList()
        if (wasActive && remaining.none { it.active }) remaining[0] = remaining[0].copy(active = true)
        store.savePlaylists(remaining)
        store.playlistCache(id).delete(); store.epgCache(id).delete()
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
        val file = store.playlistCache(active.id)
        if (!file.exists()) return null
        return runCatching {
            val parsed = M3uParser.parse(file.readText())
            CachedData(parsed.channels, emptyMap(), parsed.epgUrl)
        }.getOrNull()
    }

    private fun downloadText(url: String): String = downloadBytes(url, 8 * 1024 * 1024, readTimeoutMs = 12_000).toString(Charsets.UTF_8)

    private fun downloadBytes(url: String, maxBytes: Int, readTimeoutMs: Int): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TV-Espanol-Plus/1.3 Android")
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = connection.responseCode
            require(code in 200..299) { "Servidor respondió HTTP $code" }
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
        } finally { connection.disconnect() }
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        if (file.exists()) file.delete()
        check(tmp.renameTo(file)) { "No se pudo guardar la caché" }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
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
