package com.epalma.tvespanolplus

enum class PlaylistAuthMode { NONE, BASIC, XTREAM }

data class PlaylistConfig(
    val id: String,
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = "",
    val authMode: PlaylistAuthMode = PlaylistAuthMode.NONE,
    val active: Boolean = false,
    val lastUpdatedEpochMs: Long = 0L
) {
    // Never expose URL, username or password through accidental logs/debug toString().
    override fun toString(): String =
        "PlaylistConfig(id=$id,name=$name,authMode=$authMode,active=$active,lastUpdatedEpochMs=$lastUpdatedEpochMs)"
}

data class StreamSource(
    val url: String,
    val priority: Int = 0
)

data class Channel(
    val id: String,
    val name: String,
    val normalizedName: String,
    val tvgId: String?,
    val logo: String?,
    val group: String,
    val country: String?,
    val language: String?,
    val sources: List<StreamSource>,
    val rawAttributes: Map<String, String> = emptyMap()
)

data class Program(
    val channelTvgId: String,
    val title: String,
    val description: String?,
    val startEpochMs: Long,
    val endEpochMs: Long
) {
    fun isLive(now: Long): Boolean = now in startEpochMs until endEpochMs
    fun startsInMs(now: Long): Long = startEpochMs - now
}

data class SearchHit(
    val channel: Channel,
    val program: Program? = null,
    val score: Double,
    val temporalBucket: TemporalBucket,
    val reason: String
)

enum class TemporalBucket(val priority: Int) {
    LIVE_NOW(0),
    STARTS_SOON(1),
    TODAY(2),
    TOMORROW(3),
    UPCOMING(4),
    CHANNEL_ONLY(5),
    PAST_RELATED(6)
}

data class PlaylistParseResult(
    val channels: List<Channel>,
    val epgUrl: String?,
    val warnings: List<String> = emptyList()
)

data class RefreshResult(
    val success: Boolean,
    val message: String,
    val added: Int = 0,
    val removed: Int = 0,
    val changed: Int = 0,
    val channelCount: Int = 0
)

data class AppUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val statusMessage: String = "Preparando televisión…",
    val playlists: List<PlaylistConfig> = emptyList(),
    val activePlaylist: PlaylistConfig? = null,
    val channels: List<Channel> = emptyList(),
    val programs: Map<String, List<Program>> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchHit> = emptyList(),
    val selectedChannel: Channel? = null,
    val dualLeft: Channel? = null,
    val dualRight: Channel? = null,
    val dualAudioSide: DualSide = DualSide.LEFT,
    val dualSupported: Boolean = true,
    val lastRefresh: RefreshResult? = null,
    val error: String? = null
)

enum class DualSide { LEFT, RIGHT }

enum class Screen {
    HOME, CHANNELS, SEARCH, PLAYER, DUAL, PLAYLISTS, SETTINGS, ABOUT
}
