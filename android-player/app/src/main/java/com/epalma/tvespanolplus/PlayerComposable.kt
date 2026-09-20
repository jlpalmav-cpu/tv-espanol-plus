package com.epalma.tvespanolplus

import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private data class MediaTrackChoice(
    val type: Int,
    val group: TrackGroup,
    val index: Int,
    val label: String,
    val selected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun ResilientPlayer(
    channel: Channel,
    modifier: Modifier = Modifier,
    volume: Float = 1f,
    controls: Boolean = true,
    showTrackMenu: Boolean = false,
    onTrackMenuDismiss: () -> Unit = {},
    onTerminalError: (String) -> Unit = {},
    preferredAudioLanguage: String? = null,
    preferredSubtitleLanguage: String? = null,
    subtitleTextSizeSp: Float? = null,
    retryToken: Int = 0
) {
    val context = LocalContext.current
    val storedPrefs = remember(context) { runCatching { AppPreferenceStore(context).load() }.getOrDefault(UserPreferences()) }
    val audioPreference = preferredAudioLanguage ?: storedPrefs.preferredAudioLanguage
    val subtitlePreference = preferredSubtitleLanguage ?: storedPrefs.preferredSubtitleLanguage
    val subtitleSize = (subtitleTextSizeSp ?: storedPrefs.subtitleTextSizeSp).coerceIn(16f, 30f)
    val scope = rememberCoroutineScope()
    var sourceIndex by remember(channel.id) { mutableIntStateOf(0) }
    var reloadToken by remember(channel.id) { mutableIntStateOf(0) }
    var retryAttempt by remember(channel.id, sourceIndex) { mutableIntStateOf(0) }
    var terminalReported by remember(channel.id) { mutableStateOf(false) }
    var trackChoices by remember(channel.id) { mutableStateOf<List<MediaTrackChoice>>(emptyList()) }

    val player = remember(channel.id) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 1_000, 2_500)
            .build()
        ExoPlayer.Builder(context).setLoadControl(loadControl).build()
    }

    LaunchedEffect(channel.id, sourceIndex, reloadToken, retryToken) {
        terminalReported = false
        trackChoices = emptyList()
        val source = channel.sources.getOrNull(sourceIndex)
        if (source != null) {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(source.url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    LaunchedEffect(volume) { player.volume = volume }

    LaunchedEffect(audioPreference, subtitlePreference) {
        val builder = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

        val audio = languagePriority(audioPreference)
        if (audio.isNotEmpty()) builder.setPreferredAudioLanguages(*audio)

        if (subtitlePreference == "off") {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val subtitles = languagePriority(subtitlePreference)
            if (subtitles.isNotEmpty()) builder.setPreferredTextLanguages(*subtitles)
        }
        player.trackSelectionParameters = builder.build()
    }

    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryAttempt < 2) {
                    retryAttempt += 1
                    scope.launch {
                        delay(700L * retryAttempt)
                        reloadToken += 1
                    }
                    return
                }
                val next = PlaybackPolicy.nextSourceIndex(sourceIndex, channel.sources.size)
                if (next != null) {
                    retryAttempt = 0
                    sourceIndex = next
                } else if (!terminalReported) {
                    terminalReported = true
                    onTerminalError("Canal temporalmente no disponible. Se intentaron todas las fuentes.")
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) retryAttempt = 0
            }

            override fun onTracksChanged(tracks: Tracks) {
                trackChoices = extractChoices(tracks)
            }
        }
        player.addListener(listener)
        trackChoices = extractChoices(player.currentTracks)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                this.player = player
                useController = controls
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                keepScreenOn = true
                subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize)
            }
        },
        update = { view ->
            view.player = player
            view.useController = controls
            view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize)
        },
        modifier = modifier
    )

    if (showTrackMenu) {
        TrackMenuDialog(
            choices = trackChoices,
            dismiss = onTrackMenuDismiss,
            autoLanguage = { type, language ->
                val builder = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(type)
                    .setTrackTypeDisabled(type, false)
                val ordered = languagePriority(language)
                if (type == C.TRACK_TYPE_AUDIO && ordered.isNotEmpty()) builder.setPreferredAudioLanguages(*ordered)
                if (type == C.TRACK_TYPE_TEXT && ordered.isNotEmpty()) builder.setPreferredTextLanguages(*ordered)
                player.trackSelectionParameters = builder.build()
            },
            defaultAudioLanguage = audioPreference,
            defaultSubtitleLanguage = subtitlePreference,
            disableSubtitles = {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            },
            select = { choice ->
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(choice.type, false)
                    .setOverrideForType(TrackSelectionOverride(choice.group, choice.index))
                    .build()
            }
        )
    }
}

@Composable
private fun TrackMenuDialog(
    choices: List<MediaTrackChoice>,
    dismiss: () -> Unit,
    autoLanguage: (Int, String) -> Unit,
    defaultAudioLanguage: String,
    defaultSubtitleLanguage: String,
    disableSubtitles: () -> Unit,
    select: (MediaTrackChoice) -> Unit
) {
    val audio = choices.filter { it.type == C.TRACK_TYPE_AUDIO }
    val text = choices.filter { it.type == C.TRACK_TYPE_TEXT }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Audio y subtítulos") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                item { Text("Audio") }
                item { TextButton(onClick = { autoLanguage(C.TRACK_TYPE_AUDIO, defaultAudioLanguage); dismiss() }) { Text("Automático · ${languageLabel(defaultAudioLanguage)}") } }
                audio.forEach { choice ->
                    item(key = "a:${choice.group.hashCode()}:${choice.index}") {
                        TextButton(onClick = { select(choice); dismiss() }) { Text((if (choice.selected) "✓ " else "") + choice.label) }
                    }
                }
                item { Text("Subtítulos") }
                if (defaultSubtitleLanguage != "off") {
                    item { TextButton(onClick = { autoLanguage(C.TRACK_TYPE_TEXT, defaultSubtitleLanguage); dismiss() }) { Text("Automático · ${languageLabel(defaultSubtitleLanguage)}") } }
                }
                item { TextButton(onClick = { disableSubtitles(); dismiss() }) { Text("Desactivar subtítulos") } }
                text.forEach { choice ->
                    item(key = "t:${choice.group.hashCode()}:${choice.index}") {
                        TextButton(onClick = { select(choice); dismiss() }) { Text((if (choice.selected) "✓ " else "") + choice.label) }
                    }
                }
                if (audio.isEmpty() && text.isEmpty()) item { Text("Esta señal no publica pistas alternativas de audio o subtítulos.") }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Cerrar") } }
    )
}

private fun languagePriority(language: String): Array<String> = when (language) {
    "es-419" -> arrayOf("es-419", "es", "spa")
    "es" -> arrayOf("es", "spa", "es-419")
    "en" -> arrayOf("en", "eng")
    "pt" -> arrayOf("pt", "por")
    "fr" -> arrayOf("fr", "fra", "fre")
    else -> if (language.isBlank() || language == "auto" || language == "off") emptyArray() else arrayOf(language)
}

private fun languageLabel(language: String): String = when (language) {
    "es-419" -> "Español Latino"
    "es" -> "Español"
    "en" -> "Inglés"
    "pt" -> "Portugués"
    "fr" -> "Francés"
    "off" -> "Desactivados"
    else -> "Automático"
}

private fun extractChoices(tracks: Tracks): List<MediaTrackChoice> = buildList {
    tracks.groups.forEach { group ->
        val type = group.type
        if (type == C.TRACK_TYPE_AUDIO || type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val format = group.getTrackFormat(i)
                val language = format.language?.takeIf { it.isNotBlank() }
                val languageName = language?.let {
                    runCatching { Locale.forLanguageTag(it).getDisplayLanguage(Locale.forLanguageTag("es")) }.getOrNull()
                }?.takeIf { it.isNotBlank() }
                val label = format.label?.takeIf { it.isNotBlank() }
                val fallback = if (type == C.TRACK_TYPE_AUDIO) "Audio ${i + 1}" else "Subtítulo ${i + 1}"
                val display = listOfNotNull(label, languageName).distinct().joinToString(" · ").ifBlank { fallback }
                add(MediaTrackChoice(type, group.mediaTrackGroup, i, display, group.isTrackSelected(i)))
            }
        }
    }
}
