package com.epalma.tvespanolplus

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

object SearchEngine {
    private const val MATCH_THRESHOLD = 0.50

    fun search(
        query: String,
        channels: List<Channel>,
        programs: Map<String, List<Program>>,
        now: Long = System.currentTimeMillis(),
        limit: Int = 80
    ): List<SearchHit> {
        val q = TextNormalizer.normalize(query)
        if (q.length < 2) return emptyList()
        val qTokens = TextNormalizer.tokens(q)

        val bestPerChannel = channels.distinctBy { it.id }.mapNotNull { channel ->
            val channelText = buildString {
                append(channel.name); append(' ')
                append(channel.group); append(' ')
                append(channel.country.orEmpty()); append(' ')
                append(channel.language.orEmpty()); append(' ')
                append(channel.tvgId.orEmpty())
            }
            val channelScore = relevance(q, qTokens, channelText)
            val candidates = mutableListOf<SearchHit>()

            if (channelScore >= MATCH_THRESHOLD) {
                candidates += SearchHit(channel, null, channelScore, TemporalBucket.CHANNEL_ONLY, "Canal relacionado")
            }

            channel.tvgId?.let { id ->
                programs[id].orEmpty().forEach { program ->
                    val programScore = relevance(q, qTokens, "${program.title} ${program.description.orEmpty()}")
                    if (programScore >= MATCH_THRESHOLD) {
                        val bucket = bucket(program, now)
                        candidates += SearchHit(
                            channel,
                            program,
                            programScore + temporalBoost(program, bucket, now),
                            bucket,
                            reasonFor(bucket)
                        )
                    }
                }
            }

            candidates.minWithOrNull(
                compareBy<SearchHit> { it.temporalBucket.priority }
                    .thenByDescending { it.score }
                    .thenBy { proximity(it, now) }
            )
        }

        return bestPerChannel
            .sortedWith(
                compareBy<SearchHit> { it.temporalBucket.priority }
                    .thenByDescending { it.score }
                    .thenBy { proximity(it, now) }
                    .thenBy { TextNormalizer.normalize(it.channel.name) }
            )
            .take(limit)
    }

    fun search(
        query: String,
        channels: List<Channel>,
        programs: Map<String, List<Program>>,
        limit: Int
    ): List<SearchHit> = search(query, channels, programs, System.currentTimeMillis(), limit)

    private fun relevance(query: String, tokens: List<String>, text: String): Double {
        val norm = TextNormalizer.normalize(text)
        if (norm == query) return 1.0
        if (norm.startsWith(query)) return 0.97
        if (norm.contains(query)) return 0.94
        val words = norm.split(' ').filter { it.isNotBlank() }
        val tokenHits = tokens.count { qToken ->
            words.any { word ->
                word == qToken || word.startsWith(qToken) || TextNormalizer.fuzzySimilarity(qToken, word) >= tokenThreshold(qToken)
            }
        }
        val tokenScore = if (tokens.isEmpty()) 0.0 else tokenHits.toDouble() / tokens.size
        val fuzzyValues = tokens.mapNotNull { qToken -> words.maxOfOrNull { TextNormalizer.fuzzySimilarity(qToken, it) } }
        val bestTokenFuzzy = if (fuzzyValues.isEmpty()) 0.0 else fuzzyValues.average()
        val wholeFuzzy = TextNormalizer.fuzzySimilarity(query, norm)
        return maxOf(tokenScore * 0.92, bestTokenFuzzy * 0.84, wholeFuzzy * 0.90)
    }

    private fun tokenThreshold(token: String): Double = when {
        token.length <= 3 -> 0.92
        token.length <= 5 -> 0.80
        else -> 0.68
    }

    private fun bucket(program: Program, now: Long): TemporalBucket {
        if (program.isLive(now)) return TemporalBucket.LIVE_NOW
        val diff = program.startEpochMs - now
        if (diff in 1..(2 * 60 * 60 * 1000L)) return TemporalBucket.STARTS_SOON
        if (diff > 0) {
            val d = dayDelta(now, program.startEpochMs)
            if (d == 0) return TemporalBucket.TODAY
            if (d == 1) return TemporalBucket.TOMORROW
            return TemporalBucket.UPCOMING
        }
        return TemporalBucket.PAST_RELATED
    }

    private fun temporalBoost(program: Program, bucket: TemporalBucket, now: Long): Double = when (bucket) {
        TemporalBucket.LIVE_NOW -> 0.45
        TemporalBucket.STARTS_SOON -> 0.34 - (program.startEpochMs - now).coerceAtLeast(0L).toDouble() / (2 * 60 * 60 * 1000L) * 0.10
        TemporalBucket.TODAY -> 0.20
        TemporalBucket.TOMORROW -> 0.12
        TemporalBucket.UPCOMING -> 0.05
        TemporalBucket.CHANNEL_ONLY -> 0.0
        TemporalBucket.PAST_RELATED -> -0.08
    }

    private fun proximity(hit: SearchHit, now: Long): Long = hit.program?.let { abs(it.startEpochMs - now) } ?: Long.MAX_VALUE

    private fun dayDelta(now: Long, then: Long): Int {
        val tz = TimeZone.getDefault()
        fun dayStart(ms: Long): Long = Calendar.getInstance(tz).apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((dayStart(then) - dayStart(now)) / (24 * 60 * 60 * 1000L)).toInt()
    }

    private fun reasonFor(bucket: TemporalBucket): String = when (bucket) {
        TemporalBucket.LIVE_NOW -> "En vivo ahora"
        TemporalBucket.STARTS_SOON -> "Comienza pronto"
        TemporalBucket.TODAY -> "Hoy"
        TemporalBucket.TOMORROW -> "Mañana"
        TemporalBucket.UPCOMING -> "Próximamente"
        TemporalBucket.CHANNEL_ONLY -> "Canal relacionado"
        TemporalBucket.PAST_RELATED -> "Relacionado"
    }
}
