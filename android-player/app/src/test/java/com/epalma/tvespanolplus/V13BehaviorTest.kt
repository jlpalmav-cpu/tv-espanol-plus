package com.epalma.tvespanolplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V13BehaviorTest {
    private fun ch(id: String, name: String = "Canal $id", group: String = "General", country: String? = "HN", tvgId: String? = null) = Channel(
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

    @Test fun largeCategoryCreatesExclusiveSubcategoryFolders() {
        val channels = buildList {
            repeat(30) { add(ch("hn$it", country = "HN")) }
            repeat(25) { add(ch("mx$it", country = "MX")) }
            repeat(10) { add(ch("es$it", country = "ES")) }
            repeat(5) { add(ch("us$it", country = "US")) }
        }
        val folders = ChannelClassifier.subcategoriesForCategory(channels, "TV general")
        assertTrue(folders.isNotEmpty())
        assertEquals(70, folders.sumOf { it.count })

        val allIds = folders.flatMap { folder ->
            ChannelClassifier.channelsForSubcategory(channels, "TV general", folder.name).map { it.id }
        }
        assertEquals(70, allIds.size)
        assertEquals(allIds.size, allIds.distinct().size)
    }

    @Test fun tinyCountryGroupsCollapseIntoOtherCountries() {
        val channels = buildList {
            repeat(55) { add(ch("hn$it", country = "HN")) }
            repeat(2) { add(ch("mx$it", country = "MX")) }
            repeat(2) { add(ch("es$it", country = "ES")) }
            repeat(3) { add(ch("us$it", country = "US")) }
        }
        val folders = ChannelClassifier.subcategoriesForCategory(channels, "TV general")
        val other = folders.firstOrNull { it.name == ChannelClassifier.OTHER_COUNTRIES }
        assertNotNull(other)
        assertEquals(4, other!!.count)
    }

    @Test fun smallCategoryDoesNotCreateExtraFolders() {
        val channels = (0 until 24).map { ch("d$it", group = "Deportes", country = "HN") }
        assertTrue(ChannelClassifier.subcategoriesForCategory(channels, "Deportes").isEmpty())
    }

    @Test fun searchDoesNotTurnUnrelatedEpgIntoQueryResult() {
        val now = 1_800_000_000_000L
        val channel = ch("1", name = "FC Barcelona TV", group = "Deportes", country = "ES", tvgId = "barca.tv")
        val programs = mapOf("barca.tv" to listOf(Program("barca.tv", "Cocina mediterránea", "Recetas", now - 10_000, now + 600_000)))
        val result = SearchEngine.search("Barcelona", listOf(channel), programs, now)
        assertEquals(1, result.size)
        assertNull(result.first().program)
        assertEquals("FC Barcelona TV", result.first().channel.name)
    }

    @Test fun searchReturnsOnlyOneBestResultPerChannel() {
        val now = 1_800_000_000_000L
        val channel = ch("1", name = "Deportes Uno", group = "Deportes", country = "ES", tvgId = "sports.1")
        val programs = mapOf("sports.1" to listOf(
            Program("sports.1", "Barcelona vs Sevilla", null, now - 1000, now + 600_000),
            Program("sports.1", "Barcelona vs Valencia", null, now + 3_600_000, now + 7_200_000)
        ))
        val result = SearchEngine.search("Barcelona", listOf(channel), programs, now)
        assertEquals(1, result.size)
        assertEquals(TemporalBucket.LIVE_NOW, result.first().temporalBucket)
    }

    @Test fun fuzzyQueryStillFindsBarcelona() {
        val channel = ch("1", name = "FC Barcelona TV", group = "Deportes", country = "ES")
        val result = SearchEngine.search("fc barcelos", listOf(channel), emptyMap())
        assertTrue(result.any { it.channel.id == "1" })
    }
}
