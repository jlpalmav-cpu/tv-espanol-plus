package com.epalma.tvespanolplus

import java.net.URI
import java.net.URLDecoder

object PlaylistCleaner {
    fun clean(items: List<PlaylistConfig>): List<PlaylistConfig> {
        if (items.size < 2) return items
        val mutable = items.toMutableList()
        val toRemove = mutable.filter { candidate ->
            val name = TextNormalizer.normalize(candidate.name)
            val looksLikeUserDuplicate = name == "usuario" || name == "user" || name.startsWith("usuario ")
            looksLikeUserDuplicate && mutable.any { other ->
                other.id != candidate.id &&
                    other.authMode == PlaylistAuthMode.XTREAM &&
                    sameProvider(candidate, other)
            }
        }
        if (toRemove.isEmpty()) return items

        var result = mutable.filterNot { item -> toRemove.any { it.id == item.id } }
        val removedActive = toRemove.firstOrNull { it.active }
        if (removedActive != null && result.none { it.active }) {
            val replacement = result.indexOfFirst { it.authMode == PlaylistAuthMode.XTREAM && sameProvider(removedActive, it) }
                .takeIf { it >= 0 } ?: 0
            result = result.mapIndexed { index, item -> item.copy(active = index == replacement) }
        }
        return result
    }

    fun sameProvider(a: PlaylistConfig, b: PlaylistConfig): Boolean {
        val ka = providerKey(a)
        val kb = providerKey(b)
        if (ka.base.isBlank() || kb.base.isBlank() || ka.base != kb.base) return false
        if (ka.user.isNotBlank() && kb.user.isNotBlank() && ka.user != kb.user) return false
        if (ka.password.isNotBlank() && kb.password.isNotBlank() && ka.password != kb.password) return false
        return true
    }

    private data class ProviderKey(val base: String, val user: String, val password: String)

    private fun providerKey(p: PlaylistConfig): ProviderKey {
        val raw = p.url.trim()
        val parsed = runCatching { URI(raw) }.getOrNull()
        val host = parsed?.host.orEmpty().lowercase()
        val port = parsed?.port?.takeIf { it > 0 }?.let { ":$it" }.orEmpty()
        val path = parsed?.path.orEmpty()
            .substringBefore("/get.php")
            .substringBefore("/player_api.php")
            .trimEnd('/')
        val scheme = parsed?.scheme.orEmpty().lowercase().ifBlank { "http" }
        val base = if (host.isNotBlank()) "$scheme://$host$port$path" else raw.substringBefore('?').trimEnd('/').lowercase()
        val params = queryParams(parsed?.rawQuery.orEmpty())
        val user = p.username.ifBlank { params["username"].orEmpty() }.trim().lowercase()
        val password = p.password.ifBlank { params["password"].orEmpty() }
        return ProviderKey(base, user, password)
    }

    private fun queryParams(query: String): Map<String, String> = query.split('&')
        .mapNotNull { part ->
            val key = part.substringBefore('=', "").trim()
            if (key.isBlank()) null else key to runCatching {
                URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
            }.getOrDefault(part.substringAfter('=', ""))
        }.toMap()
}
