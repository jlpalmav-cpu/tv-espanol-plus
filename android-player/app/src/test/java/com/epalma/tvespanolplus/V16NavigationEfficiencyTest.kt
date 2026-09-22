package com.epalma.tvespanolplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class V16NavigationEfficiencyTest {
    private fun channel(id: String, name: String, group: String, country: String = "HN") = Channel(
        id = id,
        name = name,
        normalizedName = TextNormalizer.normalize(name),
        tvgId = "$id.epg",
        logo = null,
        group = group,
        country = country,
        language = "spa",
        sources = listOf(StreamSource("https://example.com/$id.m3u8"))
    )

    @Test fun sportsAreSplitBySportWithoutDuplicatingChannels() {
        val channels = buildList {
            repeat(8) { add(channel("f$it", "Fútbol Liga $it", "Deportes Fútbol", if (it % 2 == 0) "HN" else "ES")) }
            repeat(5) { add(channel("b$it", "NBA Basketball $it", "Deportes Basketball", "US")) }
            repeat(4) { add(channel("t$it", "ATP Tennis $it", "Deportes Tennis", "ES")) }
        }
        val index = CatalogIndex.build(channels)
        val folders = index.subcategories("Deportes")
        assertTrue(folders.any { it.name == "Fútbol" })
        assertTrue(folders.any { it.name == "Baloncesto" })
        assertTrue(folders.any { it.name == "Tenis" })
        assertEquals(channels.size, folders.sumOf { it.count })
        assertEquals(channels.size, folders.flatMap { index.subcategory("Deportes", it.name) }.distinctBy { it.id }.size)
    }

    @Test fun movieActionAndComedyAreDirectlyBrowsable() {
        val channels = listOf(
            channel("a1", "Cine Acción Uno", "Películas Acción"),
            channel("a2", "Action Max", "Películas Action"),
            channel("c1", "Comedia Plus", "Películas Comedia")
        )
        val index = CatalogIndex.build(channels)
        assertEquals(setOf("a1", "a2"), index.subcategory("Películas", "Acción").map { it.id }.toSet())
        assertEquals(listOf("c1"), index.subcategory("Películas", "Comedia").map { it.id })
    }

    @Test fun indexedSearchIsAccentInsensitivePartialAndUnique() {
        val sports = channel("sport1", "Fútbol Honduras HD", "Deportes Fútbol", "HN")
        val action = channel("movie1", "Acción Total", "Películas Acción", "MX")
        val index = CatalogIndex.build(listOf(sports, sports, action))
        assertEquals(2, index.entries.size)
        assertEquals("sport1", index.searchChannels("futbol hond").first().id)
        assertEquals("movie1", index.searchChannels("accion").first().id)
    }

    @Test fun catalogCacheReusesPrebuiltIndexForSameChannelList() {
        val channels = listOf(channel("1", "Canal Uno", "TV General"))
        val first = CatalogCache.get(channels)
        val second = CatalogCache.get(channels)
        assertSame(first, second)
    }

    @Test fun duplicatedUsuarioEntryIsRemovedWhenSameAsXtream() {
        val usuario = PlaylistConfig(
            id = "u",
            name = "Usuario",
            url = "https://iptv.example.com/get.php?username=leo&password=abc&type=m3u_plus",
            authMode = PlaylistAuthMode.NONE,
            active = true
        )
        val xtream = PlaylistConfig(
            id = "x",
            name = "Xtream",
            url = "https://iptv.example.com",
            username = "leo",
            password = "abc",
            authMode = PlaylistAuthMode.XTREAM,
            active = false
        )
        val cleaned = PlaylistCleaner.clean(listOf(usuario, xtream))
        assertEquals(1, cleaned.size)
        assertEquals("x", cleaned.first().id)
        assertTrue(cleaned.first().active)
    }

    @Test fun differentUserPlaylistIsNotRemoved() {
        val usuario = PlaylistConfig("u", "Usuario", "https://a.example.com", "leo", "a", PlaylistAuthMode.XTREAM)
        val other = PlaylistConfig("x", "Xtream", "https://b.example.com", "leo", "a", PlaylistAuthMode.XTREAM)
        val cleaned = PlaylistCleaner.clean(listOf(usuario, other))
        assertEquals(2, cleaned.size)
        assertFalse(cleaned.isEmpty())
    }
}
