package com.epalma.tvespanolplus

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

data class PlaylistRequest(
    val url: String,
    val authorizationHeader: String? = null
)

object PlaylistRequestResolver {
    fun validate(config: PlaylistConfig) {
        require(config.name.trim().isNotEmpty()) { "Nombre requerido" }
        val scheme = runCatching { URI(config.url.trim()).scheme?.lowercase() }.getOrNull()
        require(scheme == "https" || scheme == "http") { "La URL debe usar HTTP o HTTPS" }
        if (config.authMode != PlaylistAuthMode.NONE || config.username.isNotBlank() || config.password.isNotBlank()) {
            require(scheme == "https") { "Por seguridad, las listas con usuario o contraseña deben usar HTTPS" }
        }
        when (config.authMode) {
            PlaylistAuthMode.NONE -> Unit
            PlaylistAuthMode.BASIC, PlaylistAuthMode.XTREAM -> {
                require(config.username.isNotBlank()) { "Usuario requerido" }
                require(config.password.isNotBlank()) { "Contraseña requerida" }
            }
        }
    }

    fun playlist(config: PlaylistConfig): PlaylistRequest {
        validate(config)
        return when (config.authMode) {
            PlaylistAuthMode.NONE -> PlaylistRequest(config.url.trim())
            PlaylistAuthMode.BASIC -> PlaylistRequest(config.url.trim(), basic(config.username, config.password))
            PlaylistAuthMode.XTREAM -> PlaylistRequest(
                url = config.url.trim().trimEnd('/') + "/get.php?username=${enc(config.username)}&password=${enc(config.password)}&type=m3u_plus&output=m3u8"
            )
        }
    }

    fun epg(config: PlaylistConfig, advertisedUrl: String?): PlaylistRequest? {
        val advertised = advertisedUrl?.trim().orEmpty()
        if (advertised.isNotBlank()) {
            val request = if (config.authMode == PlaylistAuthMode.BASIC) {
                PlaylistRequest(advertised, basic(config.username, config.password))
            } else PlaylistRequest(advertised)
            return request.takeIf { isHttp(it.url) }
        }
        if (config.authMode == PlaylistAuthMode.XTREAM) {
            return PlaylistRequest(
                config.url.trim().trimEnd('/') + "/xmltv.php?username=${enc(config.username)}&password=${enc(config.password)}"
            )
        }
        return null
    }

    /** Safe UI/debug description: never returns the actual URL, username or password. */
    fun safeDescription(config: PlaylistConfig): String = when (config.authMode) {
        PlaylistAuthMode.NONE -> "URL protegida ••••••"
        PlaylistAuthMode.BASIC -> "URL + acceso básico protegidos ••••••"
        PlaylistAuthMode.XTREAM -> "Servidor Xtream + credenciales protegidos ••••••"
    }

    private fun basic(username: String, password: String): String {
        val raw = "$username:$password".toByteArray(StandardCharsets.UTF_8)
        return "Basic " + Base64.getEncoder().encodeToString(raw)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun isHttp(value: String): Boolean = runCatching {
        URI(value).scheme?.lowercase() in setOf("http", "https")
    }.getOrDefault(false)
}
