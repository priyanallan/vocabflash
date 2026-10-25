package app.vocabflash.datasetbuilder.tatoeba

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fixture-based tests for the prepared-Tatoeba source. All content is synthetic (no real Tatoeba
 * data); only the *format* mirrors the real export.
 */
class TatoebaSourceTest {

    private fun fixture(name: String): Path =
        Path.of(javaClass.getResource("/fixtures/$name")!!.toURI())

    // Surface form → wordlist lemma(s), as the pipeline builds from each lemma plus Kaikki forms.
    private val formToLemmas = mapOf(
        "haus" to setOf("haus"),
        "häuser" to setOf("haus"),
        "läuft" to setOf("laufen"),
        "laufe" to setOf("laufen"),
        "gelaufen" to setOf("laufen"),
        "lief" to setOf("laufen"),
    )

    private fun index(): TatoebaIndex =
        TatoebaSource.build(TatoebaSource.read(fixture("tatoeba-sample.tsv")), formToLemmas)

    @Test
    fun `read parses the prepared de-en tsv`() {
        val pairs = TatoebaSource.read(fixture("tatoeba-sample.tsv"))
        assertTrue(pairs.any { it.de == "Das alte Haus steht am Fluss." && it.en == "The old house stands by the river." })
    }

    @Test
    fun `select dedupes near-duplicate sentences and tags provenance`() {
        val examples = index().select("Haus", max = 10)
        // "Das Haus ist groß." appears twice in the fixture; only one survives.
        assertEquals(1, examples.count { it.de == "Das Haus ist groß." })
        assertEquals(6, examples.size)
        assertTrue(examples.all { it.source == "tatoeba" })
    }

    @Test
    fun `select prefers context diversity across inflected forms`() {
        val examples = index().select("laufen", max = 10)
        assertEquals(4, examples.size)
        // Each selected sentence uses a different inflected form of "laufen".
        assertTrue(examples.any { "läuft" in it.de })
        assertTrue(examples.any { "laufe " in it.de })
        assertTrue(examples.any { "gelaufen" in it.de })
        assertTrue(examples.any { "lief" in it.de })
    }

    @Test
    fun `select respects the max cap`() {
        assertEquals(2, index().select("Haus", max = 2).size)
        assertEquals(0, index().select("Haus", max = 0).size)
    }

    @Test
    fun `unknown lemma yields no examples`() {
        assertTrue(index().select("Fahrrad", max = 10).isEmpty())
    }
}
