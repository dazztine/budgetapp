package com.example.budgettracker.parser

/**
 * Pure Kotlin utility for Levenshtein string distance calculation and fuzzy matching.
 */
object LevenshteinMatcher {

    fun computeDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val lenA = a.length
        val lenB = b.length
        var prev = IntArray(lenB + 1) { it }
        var curr = IntArray(lenB + 1)

        for (i in 1..lenA) {
            curr[0] = i
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[lenB]
    }

    fun computeSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.trim().length, s2.trim().length)
        if (maxLen == 0) return 1.0f
        val dist = computeDistance(s1, s2)
        return (1.0f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0.0f, 1.0f)
    }

    /**
     * Acronym collision guard: short acronyms (<= 3 chars) REQUIRE exact match (distance == 0).
     * This strictly prevents "bdo" from colliding with "bpi" (distance 1).
     */
    fun isFuzzyMatch(inputToken: String, targetToken: String): Boolean {
        val a = inputToken.trim().lowercase()
        val b = targetToken.trim().lowercase()
        if (a == b) return true
        val maxLen = maxOf(a.length, b.length)
        if (maxLen <= 3) return false // Strict acronym guard!

        val dist = computeDistance(a, b)
        return when {
            maxLen in 4..6 -> dist <= 1
            else -> dist <= 2 || (dist.toFloat() / maxLen.toFloat() <= 0.30f)
        }
    }
}
