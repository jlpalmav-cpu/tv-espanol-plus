package com.epalma.tvespanolplus

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "tv_espanol_plus")

class LocalStore(private val context: Context) {
    companion object {
        const val DEFAULT_NAME = "edwinpalma@hotmail.com"
        const val DEFAULT_URL = "https://raw.githubusercontent.com/jlpalmav-cpu/tv-espanol-plus/main/TV_Espanol_Plus_VERIFICADA.m3u"
        private const val DEFAULT_ID = "default-edwinpalma"
        private val PLAYLISTS = stringPreferencesKey("playlists_json")
        private val FAVORITES = stringPreferencesKey("favorites_json")
        private val RECENTS = stringPreferencesKey("recents_json")
    }

    suspend fun ensureDefaults(): List<PlaylistConfig> {
        val existing = getPlaylists()
        if (existing.isNotEmpty()) return existing
        val initial = listOf(PlaylistConfig(DEFAULT_ID, DEFAULT_NAME, DEFAULT_URL, active = true))
        savePlaylists(initial)
        return initial
    }

    suspend fun getPlaylists(): List<PlaylistConfig> {
        val raw = context.dataStore.data.first()[PLAYLISTS] ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(PlaylistConfig(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.getString("url"),
                        active = o.optBoolean("active", false),
                        lastUpdatedEpochMs = o.optLong("lastUpdated", 0L)
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun savePlaylists(items: List<PlaylistConfig>) {
        val arr = JSONArray()
        items.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("url", p.url)
                put("active", p.active); put("lastUpdated", p.lastUpdatedEpochMs)
            })
        }
        context.dataStore.edit { it[PLAYLISTS] = arr.toString() }
    }

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

    fun playlistCache(id: String) = context.cacheDir.resolve("playlist_${safe(id)}.m3u")
    fun epgCache(id: String) = context.cacheDir.resolve("epg_${safe(id)}.xml.gz")
    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
