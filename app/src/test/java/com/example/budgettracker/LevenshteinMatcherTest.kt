package com.example.budgettracker

import com.example.budgettracker.parser.LevenshteinMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevenshteinMatcherTest {

    @Test
    fun testExactMatchDistanceIsZero() {
        assertEquals(0, LevenshteinMatcher.computeDistance("GCash", "gcash"))
        assertEquals(0, LevenshteinMatcher.computeDistance("BDO", "bdo"))
    }

    @Test
    fun testDistanceComputation() {
        assertEquals(1, LevenshteinMatcher.computeDistance("gcas", "gcash"))
        assertEquals(1, LevenshteinMatcher.computeDistance("spaylatr", "spaylater"))
        assertEquals(2, LevenshteinMatcher.computeDistance("bdo", "bpi"))
    }

    @Test
    fun testAcronymCollisionGuard() {
        // <= 3 chars MUST NOT fuzzy match if distance > 0
        assertFalse(LevenshteinMatcher.isFuzzyMatch("bdo", "bpi"))
        assertFalse(LevenshteinMatcher.isFuzzyMatch("cat", "car"))
        assertTrue(LevenshteinMatcher.isFuzzyMatch("bdo", "bdo"))
    }

    @Test
    fun testFuzzyMatchLongerTokens() {
        assertTrue(LevenshteinMatcher.isFuzzyMatch("gcas", "gcash"))
        assertTrue(LevenshteinMatcher.isFuzzyMatch("spaylatr", "spaylater"))
    }

    @Test
    fun testSimilarityRatio() {
        assertEquals(1.0f, LevenshteinMatcher.computeSimilarity("BDO", "bdo"), 0.001f)
        assertTrue(LevenshteinMatcher.computeSimilarity("gcas", "gcash") > 0.75f)
    }
}
