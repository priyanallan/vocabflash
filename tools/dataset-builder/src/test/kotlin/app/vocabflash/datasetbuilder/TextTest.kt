package app.vocabflash.datasetbuilder

import app.vocabflash.datasetbuilder.wordlist.parseDelimited
import app.vocabflash.datasetbuilder.util.articleForGender
import app.vocabflash.datasetbuilder.util.normalizeGender
import app.vocabflash.datasetbuilder.util.normalizeLemma
import app.vocabflash.datasetbuilder.util.slug
import app.vocabflash.datasetbuilder.util.stableId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextTest {

    @Test
    fun `normalizeLemma lowercases and trims`() {
        assertEquals("haus", normalizeLemma("  Haus "))
        assertEquals("laufen", normalizeLemma("Laufen"))
    }

    @Test
    fun `slug transliterates umlauts and collapses separators`() {
        assertEquals("schoen", slug("schön"))
        assertEquals("strasse", slug("Straße"))
        assertEquals("ueber", slug("über"))
        assertEquals("a-b", slug("a / b"))
        assertEquals("", slug("!!!"))
    }

    @Test
    fun `stableId is level plus slug plus pos`() {
        assertEquals("a1-haus", stableId("A1", "Haus", null))
        assertEquals("a1-haus-noun", stableId("A1", "Haus", "noun"))
        assertEquals("a2-schoen-adj", stableId("a2", "schön", "ADJ"))
    }

    @Test
    fun `gender normalization and article derivation`() {
        assertEquals("masculine", normalizeGender("m"))
        assertEquals("feminine", normalizeGender("fem"))
        assertEquals("neuter", normalizeGender("neutrum"))
        assertEquals("neuter", normalizeGender("n"))
        assertNull(normalizeGender(""))
        assertNull(normalizeGender("xyz"))
        assertEquals("der", articleForGender("masculine"))
        assertEquals("die", articleForGender("feminine"))
        assertEquals("das", articleForGender("neuter"))
        assertNull(articleForGender(null))
    }

    @Test
    fun `quoted delimited fields keep embedded delimiters`() {
        assertEquals(listOf("a", "b,c", "d"), parseDelimited("a,\"b,c\",d", ','))
        assertEquals(listOf("Haus", "\"x\"", ""), parseDelimited("Haus\t\"\"\"x\"\"\"\t", '\t'))
        assertEquals(listOf("a", "b", "c"), parseDelimited("a\tb\tc", '\t'))
    }
}
