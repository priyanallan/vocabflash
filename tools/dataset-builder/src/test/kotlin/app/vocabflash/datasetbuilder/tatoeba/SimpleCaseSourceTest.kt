package app.vocabflash.datasetbuilder.tatoeba

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fixture-based tests for the prepared, Tatoeba-derived simple case-example source. All content is
 * synthetic (no real Tatoeba data); only the *format* mirrors `tatoeba_case_examples.py`
 * output.
 */
class SimpleCaseSourceTest {

    private fun fixture(name: String): Path =
        Path.of(javaClass.getResource("/fixtures/$name")!!.toURI())

    private fun byLemma() = SimpleCaseSource.read(fixture("simple-case-sample.tsv"))

    @Test
    fun `reads the lemma-case-sentence-gloss tsv`() {
        val brot = byLemma().getValue("brot") // key is normalized (lowercased)
        assertEquals(listOf("accusative"), brot.map { it.case })
        assertEquals("Iss das Brot!", brot.single().sentence)
        assertEquals("After \"für\"", brot.single().gloss)
    }

    @Test
    fun `keeps at most one evidence per case, first occurrence wins, sorted by case`() {
        val haus = byLemma().getValue("haus")
        assertEquals(listOf("accusative", "dative"), haus.map { it.case })
        // The first accusative line wins; the later duplicate is dropped.
        assertEquals("Ich kaufe das Haus.", haus.single { it.case == "accusative" }.sentence)
    }

    @Test
    fun `an absent gloss becomes an empty string`() {
        assertTrue(byLemma().getValue("wasser").single().gloss.isEmpty())
    }

    @Test
    fun `unknown cases and malformed lines are skipped`() {
        val all = byLemma()
        assertFalse("kaputt" in all.keys, "a line whose case is not accusative/dative must be skipped")
        assertTrue(all.values.flatten().all { it.case == "accusative" || it.case == "dative" })
    }

    @Test
    fun `unknown lemma yields nothing`() {
        assertTrue(byLemma()["fahrrad"].isNullOrEmpty())
    }
}
