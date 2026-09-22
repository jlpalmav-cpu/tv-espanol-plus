package com.epalma.tvespanolplus

import java.util.Locale

data class ContentCategory(val key: String, val title: String)
data class CountryGroup(val name: String, val count: Int)
data class SubcategoryGroup(val name: String, val count: Int, val category: String)

object ChannelClassifier {
    const val SUBCATEGORY_THRESHOLD = 12
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
        val n = TextNormalizer.normalize(channel.name)
        val combined = "$g $n"
        return when {
            has(g, "adult", "18+") -> "Adultos 18+"
            has(combined, "deport", "sport", "espn", "tudn", "bein sport") -> "Deportes"
            has(g, "cine", "movie", "pelicula", "vod movie") -> "Películas"
            has(g, "serie", "telenov", "vod series") -> "Series"
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
        CatalogCache.get(channels).category(title)

    fun categoryCount(channels: List<Channel>, title: String): Int =
        CatalogCache.get(channels).category(title).size

    fun categoryCounts(channels: List<Channel>): Map<String, Int> {
        val index = CatalogCache.get(channels)
        return categories.associate { it.title to index.category(it.title).size }
    }

    /**
     * Content-aware submenus. Sports and VOD are grouped by type/genre first;
     * news and general TV are grouped by country. The groups are mutually exclusive.
     */
    fun subcategoriesForCategory(channels: List<Channel>, title: String): List<SubcategoryGroup> {
        val index = CatalogCache.get(channels)
        val items = index.category(title)
        if (items.size < SUBCATEGORY_THRESHOLD) return emptyList()
        return index.subcategories(title).map { SubcategoryGroup(it.name, it.count, title) }
    }

    fun channelsForSubcategory(channels: List<Channel>, category: String, subcategory: String): List<Channel> =
        CatalogCache.get(channels).subcategory(category, subcategory)

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

    fun countries(channels: List<Channel>): List<CountryGroup> =
        CatalogCache.get(channels).countries().map { CountryGroup(it.name, it.count) }

    fun channelsForCountry(channels: List<Channel>, country: String): List<Channel> =
        CatalogCache.get(channels).country(country)

    private fun has(text: String, vararg tokens: String): Boolean = tokens.any { TextNormalizer.normalize(it) in text }
}
