package com.epalma.tvespanolplus

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

object EpgParser {
    private val formats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    fun parse(input: InputStream, relevantIds: Set<String>, now: Long = System.currentTimeMillis()): Map<String, List<Program>> {
        val stream = if (input.markSupported()) input else input.buffered()
        stream.mark(2)
        val b1 = stream.read(); val b2 = stream.read(); stream.reset()
        val decoded = if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(stream) else stream
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(decoded, "UTF-8") }
        val out = mutableMapOf<String, MutableList<Program>>()
        val horizon = now + 7L * 24 * 60 * 60 * 1000
        var event = parser.eventType
        var currentChannel: String? = null
        var currentStart = 0L
        var currentStop = 0L
        var title = ""
        var desc: String? = null
        var insideProgramme = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        currentChannel = parser.getAttributeValue(null, "channel")
                        currentStart = parseDate(parser.getAttributeValue(null, "start"))
                        currentStop = parseDate(parser.getAttributeValue(null, "stop"))
                        title = ""; desc = null
                        insideProgramme = currentChannel != null && relevantIds.contains(currentChannel)
                    }
                    "title" -> if (insideProgramme) title = parser.nextText().trim()
                    "desc" -> if (insideProgramme) desc = parser.nextText().trim().takeIf { it.isNotBlank() }
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme" && insideProgramme) {
                    val channel = currentChannel
                    if (channel != null && title.isNotBlank() && currentStop >= now - 6 * 60 * 60 * 1000 && currentStart <= horizon) {
                        out.getOrPut(channel) { mutableListOf() } += Program(channel, title, desc, currentStart, currentStop)
                    }
                    insideProgramme = false
                }
            }
            event = parser.next()
        }
        return out.mapValues { (_, v) -> v.sortedBy { it.startEpochMs } }
    }

    internal fun parseDate(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val clean = value.trim()
        for (format in formats) runCatching { return format.parse(clean)?.time ?: 0L }
        return 0L
    }
}
