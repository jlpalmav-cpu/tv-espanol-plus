package com.epalma.tvespanolplus

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    private val punctuation = Regex("[^a-z0-9 ]+")
    private val spaces = Regex("\\s+")

    private val aliases = mapOf(
        "barca" to "barcelona",
        "fcb" to "fc barcelona",
        "fc barca" to "fc barcelona",
        "rma" to "real madrid",
        "realmadrid" to "real madrid",
        "formula1" to "formula 1",
        "f1" to "formula 1",
        "champions" to "uefa champions league",
        "ucl" to "uefa champions league"
    )

    fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val noMarks = decomposed.replace(Regex("\\p{Mn}+"), "")
        var basic = noMarks.lowercase(Locale.ROOT)
            .replace('&', ' ')
            .replace('.', ' ')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace(punctuation, " ")
            .replace(spaces, " ")
            .trim()
        repeat(2) { basic = Regex("\\b([a-z])\\s+([a-z])\\b").replace(basic, "$1$2") }
        return aliases[basic] ?: basic
    }

    fun tokens(input: String): List<String> = normalize(input).split(' ').filter { it.isNotBlank() }

    fun fuzzySimilarity(aRaw: String, bRaw: String): Double {
        val a = normalize(aRaw)
        val b = normalize(bRaw)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (b.contains(a) || a.contains(b)) return 0.92
        val dist = levenshtein(a, b)
        return (1.0 - dist.toDouble() / maxOf(a.length, b.length)).coerceIn(0.0, 1.0)
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[b.length]
    }
}
