package com.epalma.tvespanolplus

import java.util.Locale

data class ContentCategory(val key: String, val title: String)
data class CountryGroup(val name: String, val count: Int)
data class SubcategoryGroup(val name: String, val count: Int, val category: String)

object ChannelClassifier {
    const val SUBCATEGORY_THRESHOLD = 60
    private const val MIN_COUNTRY_FOLDER_SIZE = 3
    const val OTHER_COUNTRIES = "Otros países"

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

    /** Every channel resolves to exactly one canonical content category. */
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

    /**
     * Large categories are divided into mutually-exclusive country folders.
     * Tiny country groups are consolidated into "Otros países" so the user
     * does not get dozens of one-channel folders. Every channel still belongs
     * to exactly one subcategory inside its canonical category.
     */
    fun subcategoriesForCategory(channels: List<Channel>, title: String): List<SubcategoryGroup> {
        val items = channelsForCategory(channels, title)
        if (items.size < SUBCATEGORY_THRESHOLD) return emptyList()
        val grouped = items.groupBy(::countryName)
        val regular = grouped.filterValues { it.size >= MIN_COUNTRY_FOLDER_SIZE }
        val otherCount = grouped.filterValues { it.size < MIN_COUNTRY_FOLDER_SIZE }.values.sumOf { it.size }
        return buildList {
            regular.entries
                .sortedWith(compareBy<Map.Entry<String, List<Channel>>> {
                    when (TextNormalizer.normalize(it.key)) {
                        "honduras" -> 0
                        "sin pais" -> 2
                        else -> 1
                    }
                }.thenBy { TextNormalizer.normalize(it.key) })
                .forEach { add(SubcategoryGroup(it.key, it.value.size, title)) }
            if (otherCount > 0) add(SubcategoryGroup(OTHER_COUNTRIES, otherCount, title))
        }
    }

    fun channelsForSubcategory(channels: List<Channel>, category: String, subcategory: String): List<Channel> {
        val items = channelsForCategory(channels, category)
        if (subcategory == OTHER_COUNTRIES) {
            val grouped = items.groupBy(::countryName)
            val smallCountries = grouped.filterValues { it.size < MIN_COUNTRY_FOLDER_SIZE }.keys
            return items.filter { countryName(it) in smallCountries }
        }
        return items.filter { countryName(it) == subcategory }
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
