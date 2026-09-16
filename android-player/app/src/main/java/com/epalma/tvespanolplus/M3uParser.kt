package com.epalma.tvespanolplus

import java.security.MessageDigest

object M3uParser {
    private val attrRegex = Regex("([A-Za-z0-9_-]+)=\\\"([^\\\"]*)\\\"")

    fun parse(text: String): PlaylistParseResult {
        val trimmed = text.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        require(trimmed.startsWith("#EXTM3U")) { "La lista no inicia con #EXTM3U" }
        val lines = trimmed.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val epgUrl = parseEpgUrl(lines.first())
        val temp = mutableListOf<Channel>()
        val warnings = mutableListOf<String>()
        var pendingInfo: String? = null
        for (line in lines.drop(1)) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> pendingInfo = line
                !line.startsWith("#") && pendingInfo != null -> {
                    val info = pendingInfo!!
                    pendingInfo = null
                    runCatching { parseChannel(info, line) }
                        .onSuccess { temp += it }
                        .onFailure { warnings += "Entrada omitida: ${it.message ?: "inválida"}" }
                }
            }
        }
        val deduped = dedupe(temp)
        require(deduped.isNotEmpty()) { "La lista no contiene canales válidos" }
        return PlaylistParseResult(deduped, epgUrl, warnings.take(25))
    }

    private fun parseEpgUrl(header: String): String? {
        val attrs = attrRegex.findAll(header).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        return attrs["x-tvg-url"] ?: attrs["url-tvg"]
    }

    private fun parseChannel(info: String, url: String): Channel {
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) { "URL de stream no soportada" }
        val attrs = attrRegex.findAll(info).associate { it.groupValues[1].lowercase() to it.groupValues[2].trim() }
        val comma = info.indexOf(',')
        val name = if (comma >= 0) info.substring(comma + 1).trim() else attrs["tvg-name"].orEmpty()
        require(name.isNotBlank()) { "Canal sin nombre" }
        val group = attrs["group-title"].orEmpty().ifBlank { "Otros" }
        val tvgId = attrs["tvg-id"]?.takeIf { it.isNotBlank() }
        val country = attrs["tvg-country"]?.takeIf { it.isNotBlank() }
        val language = attrs["tvg-language"]?.takeIf { it.isNotBlank() }
        val stableKey = tvgId?.let { "tvg:${TextNormalizer.normalize(it)}" }
            ?: "name:${TextNormalizer.normalize(name)}|group:${TextNormalizer.normalize(group)}|country:${country.orEmpty()}"
        val id = sha256(stableKey).take(20)
        return Channel(
            id = id,
            name = name,
            normalizedName = TextNormalizer.normalize(name),
            tvgId = tvgId,
            logo = attrs["tvg-logo"]?.takeIf { it.startsWith("https://", true) },
            group = group,
            country = country,
            language = language,
            sources = listOf(StreamSource(url = url)),
            rawAttributes = attrs
        )
    }

    private fun dedupe(channels: List<Channel>): List<Channel> {
        val map = linkedMapOf<String, Channel>()
        for (c in channels) {
            val existing = map[c.id]
            if (existing == null) map[c.id] = c else {
                val urls = (existing.sources + c.sources).distinctBy { it.url }
                map[c.id] = existing.copy(
                    sources = urls.mapIndexed { index, s -> s.copy(priority = index) },
                    logo = existing.logo ?: c.logo,
                    tvgId = existing.tvgId ?: c.tvgId
                )
            }
        }
        return map.values.toList()
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
