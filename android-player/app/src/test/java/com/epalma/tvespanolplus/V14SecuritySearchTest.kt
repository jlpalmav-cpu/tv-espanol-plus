package com.epalma.tvespanolplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V14SecuritySearchTest {
    private fun ch(id: String, name: String, group: String, country: String, tvgId: String? = null) = Channel(
        id = id,
        name = name,
        normalizedName = TextNormalizer.normalize(name),
        tvgId = tvgId,
        logo = null,
        group = group,
        country = country,
        language = "spa",
        sources = listOf(StreamSource("https://example.com/$id.m3u8"))
    )

    @Test fun safePlaylistDescriptionNeverLeaksSecrets() {
        val p = PlaylistConfig(
            id = "x",
            name = "Privada",
            url = "https://secret.example/path?token=ABC123",
            username = "user@example.com",
            password = "SuperSecret!",
            authMode = PlaylistAuthMode.XTREAM
        )
        val safe = PlaylistRequestResolver.safeDescription(p)
        assertFalse(safe.contains("secret.example"))
        assertFalse(safe.contains("user@example.com"))
        assertFalse(safe.contains("SuperSecret"))
        assertFalse(p.toString().contains("secret.example"))
        assertFalse(p.toString().contains("SuperSecret"))
    }

    @Test fun xtreamBuildsAuthenticatedUrlInternally() {
        val p = PlaylistConfig("x", "Privada", "https://iptv.example", "edwin+tv", "p@ss word", PlaylistAuthMode.XTREAM)
        val request = PlaylistRequestResolver.playlist(p)
        assertTrue(request.url.startsWith("https://iptv.example/get.php?"))
        assertTrue(request.url.contains("username=edwin%2Btv"))
        assertTrue(request.url.contains("password=p%40ss+word"))
        assertTrue(request.url.contains("type=m3u_plus"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun credentialsAreRejectedOverPlainHttp() {
        PlaylistRequestResolver.validate(
            PlaylistConfig("x", "Insegura", "http://iptv.example", "user", "pass", PlaylistAuthMode.XTREAM)
        )
    }

    @Test fun multiWordSearchHonorsWholeQuery() {
        val hondurasNews = ch("1", "Canal Noticias HN", "Noticias", "HN")
        val mexicoNews = ch("2", "Canal Noticias MX", "Noticias", "MX")
        val hondurasMusic = ch("3", "Música HN", "Música", "HN")
        val result = SearchEngine.search("noticias Honduras", listOf(mexicoNews, hondurasMusic, hondurasNews), emptyMap())
        assertEquals(listOf("1"), result.map { it.channel.id })
    }

    @Test fun temporalPriorityAppliesOnlyAfterQueryMatch() {
        val now = 1_800_000_000_000L
        val barca = ch("1", "Deportes Uno", "Deportes", "ES", "sports.1")
        val unrelated = ch("2", "Noticias Hoy", "Noticias", "HN", "news.2")
        val programs = mapOf(
            "sports.1" to listOf(Program("sports.1", "Barcelona vs Sevilla", null, now + 3_600_000, now + 7_200_000)),
            "news.2" to listOf(Program("news.2", "Noticias en vivo", null, now - 1_000, now + 900_000))
        )
        val result = SearchEngine.search("Barcelona", listOf(unrelated, barca), programs, now)
        assertEquals("1", result.first().channel.id)
        assertFalse(result.any { it.channel.id == "2" })
    }
}
