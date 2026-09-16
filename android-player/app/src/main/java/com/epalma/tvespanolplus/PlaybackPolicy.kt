package com.epalma.tvespanolplus

object PlaybackPolicy {
    fun nextSourceIndex(current: Int, total: Int): Int? = if (current + 1 < total) current + 1 else null
    fun shouldAcceptRefresh(previousCount: Int, newCount: Int): Boolean {
        val minimum = if (previousCount <= 0) 10 else maxOf(10, previousCount / 4)
        return newCount >= minimum
    }
}
