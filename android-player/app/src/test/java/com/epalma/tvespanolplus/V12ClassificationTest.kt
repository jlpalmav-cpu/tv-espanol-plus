package com.epalma.tvespanolplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V12ClassificationTest {
    private fun ch(id: String, group: String, country: String? = "HN") = Channel(
        id = id,
        name = "Canal $id",
        normalizedName = "canal $id",
        tvgId = null,
        logo = null,
        group = group,
        country = country,
        language = "spa",
        sources = listOf(StreamSource("https://example.com/$id.m3u8"))
    )

    @Test fun everyChannelGetsExactlyOneCanonicalCategory() {
        val channels = listOf(
            ch("1", "01 ⚽ Deportes"),
            ch("2", "02 🎬 Cine en vivo"),
            ch("3", "03 📺 Series en vivo"),
            ch("4", "04 🙏 Cristianos y fe"),
            ch("5", "05 📰 Noticias"),
            ch("6", "06 👶 Infantil y familia"),
            ch("7", "07 🎵 Música"),
            ch("8", "08 Cultura y documentales"),
            ch("9", "09 Entretenimiento"),
            ch("10", "18+ Adultos"),
            ch("11", "Honduras")
        )
        val assigned = channels.map { it.id to ChannelClassifier.categoryFor(it) }
        assertEquals(channels.size, assigned.map { it.first }.distinct().size)
        assertTrue(assigned.all { it.second.isNotBlank() })
        assertEquals("Niños y familia", assigned.first { it.first == "6" }.second)
        assertEquals("Adultos 18+", assigned.first { it.first == "10" }.second)
        assertEquals("TV general", assigned.first { it.first == "11" }.second)
    }

    @Test fun categoryListsNeverDuplicateChannelIds() {
        val channels = listOf(ch("1", "01 Deportes"), ch("1", "01 Deportes"), ch("2", "09 Entretenimiento"))
        val allCategoryIds = ChannelClassifier.categories.flatMap { cat ->
            ChannelClassifier.channelsForCategory(channels, cat.title).map { it.id }
        }
        assertEquals(allCategoryIds.size, allCategoryIds.distinct().size)
    }

    @Test fun countriesAreFoldersWithCountsAndHondurasFirst() {
        val channels = listOf(ch("1", "01 Deportes", "HN"), ch("2", "09 Entretenimiento", "HN"), ch("3", "01 Deportes", "ES"))
        val countries = ChannelClassifier.countries(channels)
        assertEquals("Honduras", countries.first().name)
        assertEquals(2, countries.first().count)
        assertEquals(1, countries.first { it.name == "España" }.count)
    }
}
