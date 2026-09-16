package com.epalma.tvespanolplus

import java.util.Locale

data class ContentCategory(val key: String, val title: String)

data class CountryGroup(val name: String, val count: Int)

object ChannelClassifier {
    val categories = listOf(
        ContentCategory("sports", "Deportes"),
        ContentCategory("movies", "Películas"),
        ContentCategory("series", "Series"),
        ContentCategory("faith", "Cristianos"),
        ContentCategory("news", "Noticias"),
        ContentCategory("kids", "Niños y familia"),
        ContentCategory("music", "Música"),
        ContentCategory("docs", "Documentales y cultura"),
        ContentCategory("entertainment", "Entretenimiento"),
        ContentCategory("general", "TV general"),
        ContentCategory("adult", "Adultos 18+"),
        ContentCategory("other", "Otros")
    )

    /**
     * Every channel resolves to exactly one canonical content category.
     * This intentionally prevents the same channel from appearing in several
     * category grids at the same time.
     */
    fun categoryFor(channel: Channel): String {
        val g = TextNormalizer.normalize(channel.group)
        return when {
            has(g, "adult", "18+") -> "Adultos 18+"
            has(g, "deport", "sport") -> "Deportes"
            has(g, "cine", "movie", "pelicula") -> "Películas"
            has(g, "serie", "telenov") -> "Series"
            has(g, "crist", "relig", "catolic", "fe") -> "Cristianos"
            has(g, "notic", "news") -> "Noticias"
            has(g, "infantil", "kids", "nino", "familia") -> "Niños y familia"
            has(g, "music") -> "Música"
            has(g, "document", "cultura") -> "Documentales y cultura"
            has(g, "entreten") -> "Entretenimiento"
            !channel.country.isNullOrBlank() -> "TV general"
            else -> "Otros"
        }
    }

    fun channelsForCategory(channels: List<Channel>, title: String): List<Channel> =
        channels.asSequence().distinctBy { it.id }.filter { categoryFor(it) == title }.toList()

    fun categoryCount(channels: List<Channel>, title: String): Int =
        channels.asSequence().distinctBy { it.id }.count { categoryFor(it) == title }

    fun categoryCounts(channels: List<Channel>): Map<String, Int> {
        val unique = channels.distinctBy { it.id }
        return categories.associate { it.title to unique.count { channel -> categoryFor(channel) == it.title } }
    }

    fun countryName(channel: Channel): String {
        val raw = channel.country?.split(',', ';', '|')?.firstOrNull()?.trim().orEmpty()
        if (raw.isBlank()) return "Sin país"
        val code = raw.uppercase(Locale.ROOT)
        if (code.length == 2) {
            val display = Locale("", code).getDisplayCountry(Locale.forLanguageTag("es"))
            if (display.isNotBlank()) return display.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es")) else it.toString() }
        }
        return raw
    }

    fun countries(channels: List<Channel>): List<CountryGroup> {
        val counts = channels.distinctBy { it.id }.groupingBy(::countryName).eachCount()
        return counts.entries
            .map { CountryGroup(it.key, it.value) }
            .sortedWith(compareBy<CountryGroup> { if (TextNormalizer.normalize(it.name) == "honduras") 0 else 1 }
                .thenBy { TextNormalizer.normalize(it.name) })
    }

    fun channelsForCountry(channels: List<Channel>, country: String): List<Channel> =
        channels.asSequence().distinctBy { it.id }.filter { countryName(it) == country }.toList()

    private fun has(text: String, vararg tokens: String): Boolean = tokens.any { TextNormalizer.normalize(it) in text }
}
