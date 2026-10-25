package app.vocabflash.datasetbuilder

import app.vocabflash.datasetbuilder.enrich.EnrichLimits
import app.vocabflash.datasetbuilder.model.DatasetFile
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Fixture-based, end-to-end test of the parser → matcher → enricher → emitter pipeline.
 *
 * Fixtures are tiny and synthetic (no third-party content).
 */
class PipelineTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun fixture(name: String): Path =
        Path.of(javaClass.getResource("/fixtures/$name")!!.toURI())

    private fun runPipeline(kaikki: Path, out: Path): PipelineResult {
        val config = PipelineConfig(
            wordlistPath = fixture("wordlist-sample.tsv"),
            kaikkiPath = kaikki,
            outputDir = out,
            datasetVersion = "test-1",
            levels = listOf("A1", "A2", "B1"),
            limits = EnrichLimits(),
        )
        return Pipeline().run(config)
    }

    @Test
    fun `end-to-end build matches enriches and emits`() {
        val out = createTempDirectory("dataset-builder-test")
        val result = runPipeline(fixture("kaikki-sample.jsonl"), out)

        val qa = result.qa
        assertEquals(4, qa.wordlistRows)
        assertEquals(4, qa.emittedEntries)
        assertEquals(3, qa.matched)
        assertEquals(1, qa.unmatched)
        assertEquals(1L, qa.kaikkiLinesSkipped, "the 'not even json' line must be skipped")

        val byId = result.entries.associateBy { it.id }

        // wordlist had no article/gender/plural → Kaikki supplies them.
        val haus = assertNotNull(byId["a1-haus-noun"])
        assertEquals("neuter", haus.gender)
        assertEquals("das", haus.article)
        assertEquals("Häuser", haus.plural)
        assertTrue("/haʊ̯s/" in haus.ipa)
        assertEquals(1, haus.examples.size)
        assertEquals("Das Haus ist groß.", haus.examples.first().de)
        assertEquals("The house is big.", haus.examples.first().en)
        assertTrue("house" in haus.translations)
        assertTrue(haus.sources.kaikki)

        // Verb principal parts filled from Kaikki.
        val laufen = assertNotNull(byId["a1-laufen-verb"])
        val verb = assertNotNull(laufen.verb)
        assertEquals("laufen", verb.infinitive)
        assertEquals("lief", verb.praeteritum)
        assertEquals("gelaufen", verb.partizipII)
        assertTrue("to run" in laufen.translations)

        // Umlaut transliteration in the stable id.
        val schoen = assertNotNull(byId["a2-schoen-adj"])
        assertEquals("adj", schoen.pos)

        // An unmatched wordlist-only entry is still emitted.
        assertNotNull(byId["a2-unbekanntwort-noun"])
        assertEquals("A2", qa.unmatchedLemmas.single().level)

        // All four output files exist and dataset.json round-trips.
        assertContentEquals(
            listOf("dataset.json", "manifest.json", "qa-report.md", "unmatched.tsv"),
            result.writtenFiles.map { it.fileName.toString() },
        )
        val datasetText = Files.readString(out.resolve("dataset.json"))
        val dataset = json.decodeFromString<DatasetFile>(datasetText)
        assertEquals(4, dataset.entries.size)
        assertEquals("test-1", dataset.datasetVersion)
        assertEquals(3, result.manifest.matchedCount)
    }

    @Test
    fun `gzipped kaikki input is decompressed transparently`() {
        val out = createTempDirectory("dataset-builder-gz-test")
        val gz = out.resolve("kaikki.jsonl.gz")
        GZIPOutputStream(Files.newOutputStream(gz)).use { it.write(fixture("kaikki-sample.jsonl").toFile().readBytes()) }

        val result = runPipeline(gz, out.resolve("data"))
        assertEquals(3, result.qa.matched)
        assertEquals(4, result.qa.emittedEntries)
    }

    @Test
    fun `simple-case is preferred and UD fills only the missing evidence`() {
        val out = createTempDirectory("dataset-builder-case-test")
        // Simple source carries only the accusative evidence for Haus; UD has accusative + dative.
        val simple = out.resolve("simple.tsv")
        Files.writeString(simple, "Haus\taccusative\tIch kaufe das Haus.\tAfter \"für\"\n")
        val config = PipelineConfig(
            wordlistPath = fixture("wordlist-sample.tsv"),
            kaikkiPath = fixture("kaikki-sample.jsonl"),
            outputDir = out.resolve("data"),
            datasetVersion = "case-test",
            levels = listOf("A1", "A2", "B1"),
            limits = EnrichLimits(),
            simpleCasePath = simple,
            udPath = fixture("ud-sample.conllu"),
        )
        val haus = Pipeline().run(config).entries.single { it.id == "a1-haus-noun" }
        val byCase = haus.caseUsage.associateBy { it.case }
        // Accusative comes from the simpler source (differs from the UD sentence)…
        assertEquals("Ich kaufe das Haus.", byCase.getValue("accusative").sentence)
        // …while dative exists only in UD, so the fallback supplies it.
        assertEquals("Ich wohne in dem Haus.", byCase.getValue("dative").sentence)
    }

    @Test
    fun `level filter drops levels not requested`() {
        val out = createTempDirectory("dataset-builder-level-test")
        val config = PipelineConfig(
            wordlistPath = fixture("wordlist-sample.tsv"),
            kaikkiPath = fixture("kaikki-sample.jsonl"),
            outputDir = out,
            datasetVersion = "a1-only",
            levels = listOf("A1"),
            limits = EnrichLimits(),
        )
        val result = Pipeline().run(config)
        assertEquals(2, result.qa.emittedEntries) // Haus, laufen
        assertTrue(result.entries.all { it.level == "A1" })
    }
}
