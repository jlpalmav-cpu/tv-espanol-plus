package com.epalma.tvespanolplus

import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {
    @Test fun normalizerIgnoresCaseAccentsAndPunctuation() {
        assertEquals("fc barcelona", TextNormalizer.normalize("F.C. BARCÉLONA"))
        assertEquals(TextNormalizer.normalize("Fórmula 1"), TextNormalizer.normalize("Formula-1"))
    }

    @Test fun aliasesResolveSportsTerms() {
        assertEquals("barcelona", TextNormalizer.normalize("Barça"))
        assertEquals("fc barcelona", TextNormalizer.normalize("FCB"))
        assertEquals("formula 1", TextNormalizer.normalize("F1"))
    }

    @Test fun fuzzySimilarityHandlesTypos() {
        assertTrue(TextNormalizer.fuzzySimilarity("barcelos", "barcelona") > 0.65)
        assertTrue(TextNormalizer.fuzzySimilarity("barcelna", "barcelona") > 0.75)
    }

    @Test fun m3uParsesAndDeduplicatesSources() {
        val m3u = """#EXTM3U x-tvg-url=\"https://example.com/epg.xml.gz\"
#EXTINF:-1 tvg-id=\"sports.1\" tvg-name=\"Canal Uno\" group-title=\"01 Deportes\",Canal Uno
https://cdn.example.com/a.m3u8
#EXTINF:-1 tvg-id=\"sports.1\" tvg-name=\"Canal Uno HD\" group-title=\"01 Deportes\",Canal Uno HD
https://cdn.example.com/b.m3u8
"""
        val result = M3uParser.parse(m3u)
        assertEquals(1, result.channels.size)
        assertEquals(2, result.channels.first().sources.size)
        assertEquals("https://example.com/epg.xml.gz", result.epgUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun m3uRejectsUnsupportedSchemes() {
        M3uParser.parse("#EXTM3U\n#EXTINF:-1,Canal\nftp://bad.example/stream.m3u8")
    }

    @Test fun temporalSearchPutsLiveBeforeFutureAndChannelOnly() {
        val channel = Channel("1", "FC Barcelona TV", "fc barcelona tv", "barca.tv", null, "Deportes", "ES", "spa", listOf(StreamSource("https://example.com/live.m3u8")))
        val futureChannel = Channel("2", "Deportes Europa", "deportes europa", "sports.2", null, "Deportes", "ES", "spa", listOf(StreamSource("https://example.com/2.m3u8")))
        val now = 1_800_000_000_000L
        val programs = mapOf(
            "barca.tv" to listOf(Program("barca.tv", "FC Barcelona vs Sevilla", null, now - 10_000, now + 3_000_000)),
            "sports.2" to listOf(Program("sports.2", "FC Barcelona vs Valencia", null, now + 3_600_000, now + 7_200_000))
        )
        val results = SearchEngine.search("fc barcelos", listOf(futureChannel, channel), programs, now)
        assertTrue(results.isNotEmpty())
        assertEquals(TemporalBucket.LIVE_NOW, results.first().temporalBucket)
        assertEquals("1", results.first().channel.id)
    }

    @Test fun failoverStopsAfterLastSource() {
        assertEquals(1, PlaybackPolicy.nextSourceIndex(0, 2))
        assertNull(PlaybackPolicy.nextSourceIndex(1, 2))
    }

    @Test fun safeRefreshRejectsCatastrophicShrink() {
        assertTrue(PlaybackPolicy.shouldAcceptRefresh(1700, 1600))
        assertFalse(PlaybackPolicy.shouldAcceptRefresh(1700, 4))
        assertFalse(PlaybackPolicy.shouldAcceptRefresh(0, 4))
        assertTrue(PlaybackPolicy.shouldAcceptRefresh(0, 10))
    }

    @Test fun searchTenThousandChannelsIsBounded() {
        val channels = (0 until 10_000).map { i ->
            Channel("$i", if (i == 9000) "FC Barcelona TV" else "Canal $i", "canal $i", null, null, "General", null, "spa", listOf(StreamSource("https://example.com/$i.m3u8")))
        }
        val start = System.nanoTime()
        val result = SearchEngine.search("barcelna", channels, emptyMap(), limit = 20)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(result.any { it.channel.name == "FC Barcelona TV" })
        assertTrue("Search took ${elapsedMs}ms", elapsedMs < 5000)
    }
}
