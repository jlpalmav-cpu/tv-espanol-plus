package com.epalma.tvespanolplus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

data class CatalogEntry(
    val channel: Channel,
    val category: String,
    val subcategory: String,
    val country: String,
    val searchText: String,
    val words: Set<String>
)

data class CatalogFolder(val name: String, val count: Int)

class CatalogIndex private constructor(
    val entries: List<CatalogEntry>,
    private val byId: Map<String, CatalogEntry>,
    private val byCategory: Map<String, List<CatalogEntry>>,
    private val byCategorySubcategory: Map<Pair<String, String>, List<CatalogEntry>>,
    private val byCountry: Map<String, List<CatalogEntry>>
) {
    fun channel(id: String): Channel? = byId[id]?.channel

    fun category(title: String): List<Channel> = byCategory[title].orEmpty().map { it.channel }

    fun subcategory(category: String, subcategory: String): List<Channel> =
        byCategorySubcategory[category to subcategory].orEmpty().map { it.channel }

    fun country(name: String): List<Channel> = byCountry[name].orEmpty().map { it.channel }

    fun subcategories(category: String): List<CatalogFolder> {
        val source = byCategory[category].orEmpty()
        if (source.isEmpty()) return emptyList()
        val grouped = source.groupBy { it.subcategory }
        if (grouped.size <= 1) return emptyList()
        return grouped.entries
            .map { CatalogFolder(it.key, it.value.size) }
            .sortedWith(compareBy<CatalogFolder> { SmartTaxonomy.order(category, it.name) }
                .thenBy { TextNormalizer.normalize(it.name) })
    }

    fun countries(): List<CatalogFolder> = byCountry.entries
        .map { CatalogFolder(it.key, it.value.size) }
        .sortedWith(compareBy<CatalogFolder> {
            when (TextNormalizer.normalize(it.name)) {
                "honduras" -> 0
                "sin pais" -> 2
                else -> 1
            }
        }.thenBy { TextNormalizer.normalize(it.name) })

    fun categoriesForCountry(country: String): List<CatalogFolder> {
        val items = byCountry[country].orEmpty()
        return items.groupBy { it.category }
            .entries
            .map { CatalogFolder(it.key, it.value.size) }
            .sortedBy { folder -> ChannelClassifier.categories.indexOfFirst { it.title == folder.name }.let { if (it < 0) 999 else it } }
    }

    /**
     * Fast channel-only search. All expensive normalization is done once while the index is built.
     * EPG results can still be appended asynchronously by SearchEngine without blocking navigation.
     */
    fun searchChannels(query: String, limit: Int = 60): List<Channel> {
        val q = TextNormalizer.normalize(query)
        if (q.length < 2) return emptyList()
        val tokens = TextNormalizer.tokens(q)
        return entries.asSequence()
            .mapNotNull { entry ->
                val score = score(q, tokens, entry)
                if (score <= 0) null else entry to score
            }
            .sortedWith(compareByDescending<Pair<CatalogEntry, Int>> { it.second }
                .thenBy { TextNormalizer.normalize(it.first.channel.name) })
            .take(limit)
            .map { it.first.channel }
            .toList()
    }

    private fun score(query: String, tokens: List<String>, entry: CatalogEntry): Int {
        val name = entry.channel.normalizedName.ifBlank { TextNormalizer.normalize(entry.channel.name) }
        if (name == query) return 1200
        if (name.startsWith(query)) return 1100
        if (name.contains(query)) return 1000
        if (entry.searchText.contains(query)) return 900
        if (tokens.isNotEmpty() && tokens.all { token -> entry.words.any { word -> word == token || word.startsWith(token) } }) return 820
        if (tokens.size > 1) {
            val covered = tokens.count { token -> entry.words.any { word -> word == token || word.startsWith(token) || token.startsWith(word) } }
            if (covered >= max(1, (tokens.size * 3 + 3) / 4)) return 690 + covered * 10
        }
        if (query.length >= 4) {
            val similarity = TextNormalizer.fuzzySimilarity(query, name)
            if (similarity >= 0.72) return (similarity * 600).toInt()
        }
        return 0
    }

    companion object {
        val EMPTY = CatalogIndex(emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

        fun build(channels: List<Channel>): CatalogIndex {
            val unique = channels.distinctBy { it.id }
            val entries = unique.map { channel ->
                val category = ChannelClassifier.categoryFor(channel)
                val country = ChannelClassifier.countryName(channel)
                val sub = SmartTaxonomy.subcategory(channel, category, country)
                val raw = buildString {
                    append(channel.name); append(' ')
                    append(channel.group); append(' ')
                    append(category); append(' ')
                    append(sub); append(' ')
                    append(country); append(' ')
                    append(channel.language.orEmpty()); append(' ')
                    append(channel.tvgId.orEmpty()); append(' ')
                    channel.rawAttributes.values.forEach { append(it); append(' ') }
                }
                val normalized = TextNormalizer.normalize(raw)
                CatalogEntry(
                    channel = channel,
                    category = category,
                    subcategory = sub,
                    country = country,
                    searchText = normalized,
                    words = normalized.split(' ').filter { it.isNotBlank() }.toSet()
                )
            }
            return CatalogIndex(
                entries = entries,
                byId = entries.associateBy { it.channel.id },
                byCategory = entries.groupBy { it.category },
                byCategorySubcategory = entries.groupBy { it.category to it.subcategory },
                byCountry = entries.groupBy { it.country }
            )
        }
    }
}

/** A one-entry cache keyed by the channel-list instance used by AppUiState. */
object CatalogCache {
    @Volatile private var source: List<Channel>? = null
    @Volatile private var index: CatalogIndex = CatalogIndex.EMPTY

    fun get(channels: List<Channel>): CatalogIndex {
        if (source === channels) return index
        return synchronized(this) {
            if (source !== channels) {
                index = CatalogIndex.build(channels)
                source = channels
            }
            index
        }
    }

    suspend fun prewarm(channels: List<Channel>) = withContext(Dispatchers.Default) { get(channels) }
}

object SmartTaxonomy {
    private val sports = linkedMapOf(
        "Fútbol" to listOf("futbol", "football", "soccer", "liga", "laliga", "premier", "champions", "uefa", "fifa", "mundial", "copa libertadores"),
        "Baloncesto" to listOf("balonc", "basket", "nba", "wnba"),
        "Béisbol" to listOf("beisbol", "baseball", "mlb"),
        "Tenis" to listOf("tenis", "tennis", "atp", "wta", "grand slam"),
        "Automovilismo" to listOf("formula 1", "formula1", "f1", "motogp", "nascar", "automovil", "rally", "indycar"),
        "Boxeo / MMA" to listOf("boxeo", "boxing", "mma", "ufc", "combate"),
        "Golf" to listOf("golf", "pga"),
        "Ciclismo" to listOf("ciclismo", "cycling", "tour de france")
    )

    private val movies = linkedMapOf(
        "Acción" to listOf("accion", "action", "aventura"),
        "Comedia" to listOf("comedia", "comedy"),
        "Drama" to listOf("drama"),
        "Terror" to listOf("terror", "horror"),
        "Suspenso" to listOf("suspenso", "thriller"),
        "Ciencia ficción" to listOf("ciencia ficcion", "sci fi", "scifi", "science fiction"),
        "Infantil / Familiar" to listOf("infantil", "kids", "familia", "family"),
        "Animación" to listOf("animacion", "animation", "anime"),
        "Romance" to listOf("romance", "romant"),
        "Documentales" to listOf("documental", "documentary")
    )

    private val series = linkedMapOf(
        "Acción" to listOf("accion", "action", "aventura"),
        "Comedia" to listOf("comedia", "comedy", "sitcom"),
        "Drama" to listOf("drama"),
        "Crimen" to listOf("crimen", "crime", "policial"),
        "Ciencia ficción" to listOf("ciencia ficcion", "sci fi", "scifi", "fantasia", "fantasy"),
        "Telenovelas" to listOf("telenov", "novela"),
        "Anime" to listOf("anime"),
        "Infantil / Familiar" to listOf("infantil", "kids", "familia", "family")
    )

    fun subcategory(channel: Channel, category: String, country: String): String {
        val text = TextNormalizer.normalize(buildString {
            append(channel.name); append(' '); append(channel.group); append(' ')
            channel.rawAttributes.values.forEach { append(it); append(' ') }
        })
        return when (category) {
            "Deportes" -> match(text, sports) ?: "Otros deportes"
            "Películas" -> match(text, movies) ?: "Otras películas"
            "Series" -> match(text, series) ?: "Otras series"
            "Noticias" -> country
            "TV general" -> country
            else -> country
        }
    }

    fun order(category: String, name: String): Int {
        val list = when (category) {
            "Deportes" -> sports.keys.toList() + "Otros deportes"
            "Películas" -> movies.keys.toList() + "Otras películas"
            "Series" -> series.keys.toList() + "Otras series"
            else -> listOf("Honduras")
        }
        val index = list.indexOf(name)
        return if (index >= 0) index else if (TextNormalizer.normalize(name) == "honduras") 0 else 500
    }

    private fun match(text: String, groups: LinkedHashMap<String, List<String>>): String? =
        groups.entries.firstOrNull { (_, tokens) -> tokens.any { TextNormalizer.normalize(it) in text } }?.key
}
