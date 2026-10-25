package com.priyanallan.vocabflash.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Examples shown must stay short enough to read at the Entry's level (roughly two lines). */
class ExampleFilterTest {

    private fun entry(level: Level, vararg sentences: String) = Entry(
        id = "x",
        lemma = "x",
        partOfSpeech = PartOfSpeech.Noun,
        level = level,
        examples = sentences.map { ExampleSentence(de = it) },
    )

    @Test
    fun dropsSentencesAboveTheLevelCap() {
        val short = "Kurzer Satz."
        val tooLong = "x".repeat(200)
        val filtered = entry(Level.A1, short, tooLong).readableExamples()
        assertEquals(listOf(short), filtered.map { it.de })
    }

    @Test
    fun capsTightenForLowerLevels() {
        val eighty = "y".repeat(80)
        assertTrue(entry(Level.A1, eighty).readableExamples().isEmpty())
        assertTrue(entry(Level.A2, eighty).readableExamples().isNotEmpty())
        assertTrue(entry(Level.B1, eighty).readableExamples().isNotEmpty())
    }

    @Test
    fun returnsShortestFirstAndRespectsTheLimit() {
        val e = entry(Level.B1, "aaa.", "bb.", "cccccccc.")
        assertEquals(listOf("bb.", "aaa.", "cccccccc."), e.readableExamples().map { it.de })
        assertEquals(2, e.readableExamples(limit = 2).size)
    }

    @Test
    fun emptyWhenNothingIsShortEnough() {
        assertTrue(entry(Level.A1, "z".repeat(120)).readableExamples().isEmpty())
    }
}
