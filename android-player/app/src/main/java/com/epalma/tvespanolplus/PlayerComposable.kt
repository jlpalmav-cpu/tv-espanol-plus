package com.epalma.tvespanolplus

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun ResilientPlayer(
    channel: Channel,
    modifier: Modifier = Modifier,
    volume: Float = 1f,
    controls: Boolean = true,
    onTerminalError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var sourceIndex by remember(channel.id) { mutableIntStateOf(0) }
    var terminalReported by remember(channel.id) { mutableStateOf(false) }

    val player = remember(channel.id) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 1_000, 2_500)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                trackSelectionParameters = TrackSelectionParameters.Builder(context)
                    .setPreferredAudioLanguages("es-419", "es", "spa")
                    .build()
            }
    }

    LaunchedEffect(channel.id, sourceIndex) {
        terminalReported = false
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

    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val next = PlaybackPolicy.nextSourceIndex(sourceIndex, channel.sources.size)
                if (next != null) sourceIndex = next else if (!terminalReported) {
                    terminalReported = true
                    onTerminalError("Canal temporalmente no disponible")
                }
            }
        }
        player.addListener(listener)
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
            }
        },
        update = { view ->
            view.player = player
            view.useController = controls
        },
        modifier = modifier
    )
}
