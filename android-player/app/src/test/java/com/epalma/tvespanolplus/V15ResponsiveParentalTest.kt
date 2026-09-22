package com.epalma.tvespanolplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V15ResponsiveParentalTest {
    private fun channel(
        id: String,
        name: String,
        group: String,
        country: String = "HN",
        tvgId: String? = null
    ) = Channel(
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

    @Test fun parentalPinRequiresFourToSixDigits() {
        assertFalse(ParentalPolicy.validPinFormat("123"))
        assertTrue(ParentalPolicy.validPinFormat("1234"))
        assertTrue(ParentalPolicy.validPinFormat("123456"))
        assertFalse(ParentalPolicy.validPinFormat("1234567"))
        assertFalse(ParentalPolicy.validPinFormat("12a4"))
    }

    @Test fun parentalPinIsStoredAsHashNotPlainText() {
        val hash = AppPreferenceStore.hashPin("4826")
        assertNotEquals("4826", hash)
        assertEquals(64, hash.length)
        assertEquals(hash, AppPreferenceStore.hashPin("4826"))
    }

    @Test fun parentalPolicyBlocksAdultAndManualChannelOnlyWhenEnabled() {
        val adult = channel("adult", "Canal Adulto", "Adultos 18+")
        val news = channel("news", "Noticias HN", "Noticias")
        val off = UserPreferences(parentalEnabled = false, lockedChannelIds = setOf("news"))
        assertFalse(ParentalPolicy.isRestrictedChannel(adult, off))
        assertFalse(ParentalPolicy.isRestrictedChannel(news, off))

        val on = UserPreferences(parentalEnabled = true, parentalPinHash = "hash", lockedChannelIds = setOf("news"))
        assertTrue(ParentalPolicy.isRestrictedChannel(adult, on))
        assertTrue(ParentalPolicy.isRestrictedChannel(news, on))
        assertTrue(ParentalPolicy.isRestrictedCategory("Adultos 18+", on))
        assertTrue(ParentalPolicy.isRestrictedCategory("CountryCat:Honduras|Adultos 18+", on))
        assertFalse(ParentalPolicy.isRestrictedCategory("Noticias", on))
    }

    @Test fun m3uDuplicatesBecomeOneChannelWithMultipleSources() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id="canal5.hn" tvg-country="HN" group-title="TV General",Canal 5 HD
            https://example.com/canal5-a.m3u8
            #EXTINF:-1 tvg-id="canal5.hn" tvg-country="HN" group-title="TV General",Canal 5 FHD
            https://example.com/canal5-b.m3u8
        """.trimIndent()
        val parsed = M3uParser.parse(text)
        assertEquals(1, parsed.channels.size)
        assertEquals(2, parsed.channels.first().sources.size)
    }

    @Test fun submenuCountsEqualUniqueCanonicalCategoryCount() {
        val channels = buildList {
            repeat(8) { index -> add(channel("f$index", "Fútbol $index", "Deportes Fútbol", if (index % 2 == 0) "HN" else "ES")) }
            repeat(5) { index -> add(channel("b$index", "NBA $index", "Deportes Basketball", "US")) }
            repeat(5) { index -> add(channel("t$index", "ATP Tennis $index", "Deportes Tennis", "ES")) }
        }
        val groups = ChannelClassifier.subcategoriesForCategory(channels, "Deportes")
        assertTrue(groups.isNotEmpty())
        assertEquals(ChannelClassifier.categoryCount(channels, "Deportes"), groups.sumOf { it.count })
        val allIds = groups.flatMap { group -> ChannelClassifier.channelsForSubcategory(channels, "Deportes", group.name).map { it.id } }
        assertEquals(allIds.size, allIds.distinct().size)
    }

    @Test fun searchAlwaysReturnsOneBestHitPerChannel() {
        val now = 1_800_000_000_000L
        val sports = channel("1", "Deportes Uno", "Deportes", "ES", "sports.1")
        val programs = mapOf(
            "sports.1" to listOf(
                Program("sports.1", "Barcelona vs Madrid", null, now - 1_000, now + 900_000),
                Program("sports.1", "Barcelona resumen", null, now + 3_600_000, now + 7_200_000)
            )
        )
        val result = SearchEngine.search("Barcelona", listOf(sports, sports), programs, now)
        assertEquals(1, result.size)
        assertEquals("1", result.first().channel.id)
    }
}
