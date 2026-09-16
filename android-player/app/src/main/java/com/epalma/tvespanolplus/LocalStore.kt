package com.epalma.tvespanolplus

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "tv_espanol_plus")

class LocalStore(private val context: Context) {
    companion object {
        const val DEFAULT_NAME = "edwinpalma@hotmail.com"
        const val DEFAULT_URL = "https://raw.githubusercontent.com/jlpalmav-cpu/tv-espanol-plus/main/TV_Espanol_Plus_VERIFICADA.m3u"
        const val DEFAULT_ID = "default-edwinpalma"
        private const val SECURE_PLAYLISTS = "playlists_json_v2"
        private val LEGACY_PLAYLISTS = stringPreferencesKey("playlists_json")
        private val FAVORITES = stringPreferencesKey("favorites_json")
        private val RECENTS = stringPreferencesKey("recents_json")
    }

    private val secure = SecureStore(context)

    suspend fun ensureDefaults(): List<PlaylistConfig> {
        val existing = getPlaylists()
        val hasDefault = existing.any { it.id == DEFAULT_ID }
        val normalized = existing.map {
            if (it.id == DEFAULT_ID) it.copy(
                name = DEFAULT_NAME,
                url = DEFAULT_URL,
                username = "",
                password = "",
                authMode = PlaylistAuthMode.NONE
            ) else it
        }.toMutableList()

        if (!hasDefault) {
            val makeActive = normalized.none { it.active }
            normalized.add(0, PlaylistConfig(DEFAULT_ID, DEFAULT_NAME, DEFAULT_URL, active = makeActive))
        }
        if (normalized.isEmpty()) normalized += PlaylistConfig(DEFAULT_ID, DEFAULT_NAME, DEFAULT_URL, active = true)
        if (normalized.none { it.active }) normalized[0] = normalized[0].copy(active = true)
        if (existing != normalized) savePlaylists(normalized)
        return normalized
    }

    suspend fun getPlaylists(): List<PlaylistConfig> {
        secure.getString(SECURE_PLAYLISTS)?.let { encryptedJson ->
            return decodePlaylists(encryptedJson)
        }

        // One-time migration from the previous plaintext DataStore format.
        val legacy = context.dataStore.data.first()[LEGACY_PLAYLISTS]
        if (!legacy.isNullOrBlank()) {
            val migrated = decodePlaylists(legacy)
            savePlaylists(migrated)
            context.dataStore.edit { it.remove(LEGACY_PLAYLISTS) }
            return migrated
        }
        return emptyList()
    }

    suspend fun savePlaylists(items: List<PlaylistConfig>) {
        val arr = JSONArray()
        items.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("url", p.url)
                put("username", p.username)
                put("password", p.password)
                put("authMode", p.authMode.name)
                put("active", p.active)
                put("lastUpdated", p.lastUpdatedEpochMs)
            })
        }
        secure.putString(SECURE_PLAYLISTS, arr.toString())
        // Ensure old plaintext copy is gone after every save.
        context.dataStore.edit { it.remove(LEGACY_PLAYLISTS) }
    }

    private fun decodePlaylists(raw: String): List<PlaylistConfig> = runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val auth = runCatching { PlaylistAuthMode.valueOf(o.optString("authMode", "NONE")) }
                    .getOrDefault(PlaylistAuthMode.NONE)
                add(
                    PlaylistConfig(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.optString("url", ""),
                        username = o.optString("username", ""),
                        password = o.optString("password", ""),
                        authMode = auth,
                        active = o.optBoolean("active", false),
                        lastUpdatedEpochMs = o.optLong("lastUpdated", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    suspend fun favorites(): Set<String> = decodeArray(context.dataStore.data.first()[FAVORITES]).toSet()
    suspend fun saveFavorites(ids: Set<String>) = context.dataStore.edit { it[FAVORITES] = JSONArray(ids.toList()).toString() }
    suspend fun recents(): List<String> = decodeArray(context.dataStore.data.first()[RECENTS]).take(20)
    suspend fun saveRecents(ids: List<String>) = context.dataStore.edit { it[RECENTS] = JSONArray(ids.take(20)).toString() }

    private fun decodeArray(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun playlistCache(id: String) = context.cacheDir.resolve("playlist_${safe(id)}.secure")
    fun epgCache(id: String) = context.cacheDir.resolve("epg_${safe(id)}.secure")

    fun writeEncrypted(file: File, plain: ByteArray) {
        atomicWrite(file, secure.encryptBytes(plain))
    }

    fun readEncrypted(file: File): ByteArray? {
        if (!file.exists()) return null
        val raw = runCatching { file.readBytes() }.getOrNull() ?: return null
        return runCatching { secure.decryptBytes(raw) }.getOrElse {
            // Upgrade a legacy plaintext cache in place, then return it.
            runCatching { writeEncrypted(file, raw) }
            raw
        }
    }

    fun deleteSensitiveCaches(id: String) {
        playlistCache(id).delete()
        epgCache(id).delete()
        // Legacy filenames from older builds.
        context.cacheDir.resolve("playlist_${safe(id)}.m3u").delete()
        context.cacheDir.resolve("epg_${safe(id)}.xml.gz").delete()
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        if (file.exists()) file.delete()
        check(tmp.renameTo(file)) { "No se pudo guardar la caché segura" }
    }

    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
