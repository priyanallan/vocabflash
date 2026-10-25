package app.vocabflash.datasetbuilder

import app.vocabflash.datasetbuilder.emit.DatasetWriter
import app.vocabflash.datasetbuilder.emit.Manifest
import app.vocabflash.datasetbuilder.emit.QaReport
import app.vocabflash.datasetbuilder.emit.SCHEMA_VERSION
import app.vocabflash.datasetbuilder.emit.Sources
import app.vocabflash.datasetbuilder.emit.Tool
import app.vocabflash.datasetbuilder.emit.UnmatchedLemma
import app.vocabflash.datasetbuilder.enrich.EnrichLimits
import app.vocabflash.datasetbuilder.enrich.Enricher
import app.vocabflash.datasetbuilder.wordlist.WordListSource
import app.vocabflash.datasetbuilder.kaikki.CompactKaikki
import app.vocabflash.datasetbuilder.kaikki.KaikkiIndex
import app.vocabflash.datasetbuilder.kaikki.KaikkiReader
import app.vocabflash.datasetbuilder.match.MatchResult
import app.vocabflash.datasetbuilder.match.Matcher
import app.vocabflash.datasetbuilder.model.DatasetEntry
import app.vocabflash.datasetbuilder.model.DatasetFile
import app.vocabflash.datasetbuilder.odenet.OdeNetSource
import app.vocabflash.datasetbuilder.tatoeba.SimpleCaseSource
import app.vocabflash.datasetbuilder.tatoeba.TatoebaIndex
import app.vocabflash.datasetbuilder.tatoeba.TatoebaSource
import app.vocabflash.datasetbuilder.ud.UdCaseSource
import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/** All pipeline inputs, resolved from CLI arguments. No path is hard-coded. */
data class PipelineConfig(
    val wordlistPath: Path,
    val kaikkiPath: Path,
    val outputDir: Path,
    val datasetVersion: String,
    val levels: List<String>,
    val limits: EnrichLimits,
    val rowLimit: Int? = null,
    /** Optional enrichment sources; when absent (null or unreadable) the pipeline still runs. */
    val tatoebaPath: Path? = null,
    val odenetPath: Path? = null,
    val udPath: Path? = null,
    /**
     * Optional Tatoeba-derived *simple* case examples. When supplied it is preferred over [udPath]:
     * its sentences are curated to be short and unambiguous (better at A1/A2), while UD is only a
     * fallback for lemmas it has no evidence for.
     */
    val simpleCasePath: Path? = null,
)

/** Longest example sentence kept per CEFR level (roughly two lines on a phone). */
internal fun maxExampleChars(level: String): Int = when (level.trim().uppercase()) {
    "A1" -> 70
    "A2" -> 85
    "B1" -> 110
    "B2" -> 130
    else -> 130
}

/** Outcome of a pipeline run, for logging/testing. */
data class PipelineResult(
    val entries: List<DatasetEntry>,
    val manifest: Manifest,
    val qa: QaReport,
    val writtenFiles: List<Path>,
)

/**
 * The dataset pipeline: wordlist (level authority) → match against Kaikki → enrich → emit.
 *
 * Optional enrichment stages (Tatoeba examples, OdeNet relations, UD case evidence) run after the
 * Kaikki merge and are absent-safe: an absent or unreadable input is skipped with a QA note rather
 * than failing the run.
 *
 * This class is pure orchestration; each stage lives in its own package so it can be tested in
 * isolation and swapped later (e.g. a different enrichment source).
 */
class Pipeline(private val toolVersion: String = "0.1.0") {

    fun run(config: PipelineConfig): PipelineResult {
        val allowedLevels = config.levels.map { it.uppercase() }.toSet()
        val notes = ArrayList<String>()

        // 1) Import the normalized wordlist (level authority).
        val rows = WordListSource.read(config.wordlistPath, allowedLevels)
            .let { if (config.rowLimit != null) it.take(config.rowLimit) else it }

        // 2) Stream Kaikki into a lemma index (bounded memory; whole file is never buffered).
        val index = KaikkiIndex()
        val streamStats = KaikkiReader.stream(config.kaikkiPath) { index.add(it) }

        // 3) Match + enrich.
        val matcher = Matcher(index::lookup)
        val entries = ArrayList<DatasetEntry>(rows.size)
        val unmatched = ArrayList<UnmatchedLemma>()
        val idCollisions = ArrayList<String>()
        val seenIds = HashMap<String, Int>()
        // Surface form (normalized) → wordlist lemma(s), for locating inflected forms in Tatoeba.
        val formToLemmas = HashMap<String, MutableSet<String>>()
        var matched = 0

        for (row in rows) {
            val kaikki = when (val m = matcher.match(row)) {
                is MatchResult.Matched -> { matched++; m.entry }
                is MatchResult.Unmatched -> { unmatched.add(UnmatchedLemma(row.level, m.lemma)); null }
            }

            val entry = Enricher.enrich(row, kaikki, config.limits)
            registerForms(formToLemmas, row.lemma, kaikki)

            // Guarantee id uniqueness while keeping ids deterministic. Collisions happen only when
            // the same lemma+level+pos repeats in the source; they are reported, never hidden.
            val seen = seenIds[entry.id]
            val finalEntry = if (seen == null) {
                seenIds[entry.id] = 1
                entry
            } else {
                seenIds[entry.id] = seen + 1
                val suffixed = entry.copy(id = "${entry.id}-$seen")
                idCollisions.add("${entry.id} → ${suffixed.id} (${entry.level}: ${entry.lemma})")
                suffixed
            }
            entries.add(finalEntry)
        }

        // 4) Optional enrichment stages (absent-safe).
        val tatoebaIndex = tryLoad(config.tatoebaPath, "Tatoeba", notes) { path ->
            TatoebaSource.build(TatoebaSource.read(path), formToLemmas)
        }
        val odenetByLemma = tryLoad(config.odenetPath, "OdeNet", notes, OdeNetSource::read)
        val simpleCaseByLemma = tryLoad(config.simpleCasePath, "Tatoeba case examples", notes, SimpleCaseSource::read)
        val udByLemma = tryLoad(config.udPath, "UD German GSD", notes, UdCaseSource::read)
        if (simpleCaseByLemma != null && udByLemma != null) {
            notes.add(
                "Both --simple-case and --ud supplied: --simple-case is preferred and UD is used " +
                    "only as a fallback for lemmas without simple-case evidence.",
            )
        }

        val enriched = entries.map { e ->
            var out = e
            if (tatoebaIndex != null) {
                out = Enricher.withTatoeba(out, tatoebaIndex.select(out.lemma, config.limits.maxTatoebaExamples))
            }
            // Drop examples too long to read at the entry's level, and keep the shortest ones.
            // Doing this here keeps the bundled dataset small (the app then needs no filtering).
            out = out.copy(
                examples = out.examples
                    .filter { it.de.length <= maxExampleChars(out.level) }
                    .sortedBy { it.de.length }
                    .take(config.limits.maxExamples + config.limits.maxTatoebaExamples),
            )
            if (odenetByLemma != null) {
                out = Enricher.withRelated(out, odenetByLemma[normalizeLemma(out.lemma)].orEmpty(), config.limits.maxRelated)
            }
            // Prefer the simpler Tatoeba-derived examples PER CASE; UD only fills cases they lack.
            val normLemma = normalizeLemma(out.lemma)
            val simpleCases = simpleCaseByLemma?.get(normLemma).orEmpty()
            val udCases = udByLemma?.get(normLemma).orEmpty()
            val caseEvidence = if (simpleCases.isEmpty()) {
                udCases
            } else {
                val covered = simpleCases.mapTo(mutableSetOf()) { it.case.lowercase() }
                simpleCases + udCases.filter { it.case.lowercase() !in covered }
            }
            out = Enricher.withCaseUsage(out, caseEvidence)
            out
        }

        val orderedLevels = config.levels.map { it.uppercase() }
        val sortedEntries = enriched.sortedWith(
            compareBy(
                { orderedLevels.indexOf(it.level).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                { it.normalizedLemma },
                { it.id },
            ),
        )

        val generatedAt = Instant.now().toString()
        val perLevel = orderedLevels.associateWith { lvl -> sortedEntries.count { it.level == lvl } }
        val matchRate = if (rows.isEmpty()) 0.0 else matched.toDouble() / rows.size

        val dataset = DatasetFile(
            schemaVersion = SCHEMA_VERSION,
            datasetVersion = config.datasetVersion,
            generatedAt = generatedAt,
            levels = orderedLevels,
            entries = sortedEntries,
        )

        val licenses = buildList {
            add("Level-tagged wordlist: supplied by the caller (not bundled); use sources you may redistribute")
            add("Kaikki/Wiktextract enrichment: CC BY-SA 4.0 (attribution + share-alike)")
            if (tatoebaIndex != null) add("Tatoeba example sentences: CC BY 2.0 FR (attribution)")
            if (simpleCaseByLemma != null) add("Tatoeba-derived simple case examples: CC BY 2.0 FR (attribution)")
            if (odenetByLemma != null) add("OdeNet (Open German WordNet): CC BY-SA 4.0 (attribution + share-alike)")
            if (udByLemma != null) add("Universal Dependencies German GSD: CC BY-SA 4.0 (attribution + share-alike)")
        }

        val manifest = Manifest(
            schemaVersion = SCHEMA_VERSION,
            datasetVersion = config.datasetVersion,
            generatedAt = generatedAt,
            levels = orderedLevels,
            entryCount = sortedEntries.size,
            matchedCount = matched,
            unmatchedCount = unmatched.size,
            matchRate = matchRate,
            perLevel = perLevel,
            sources = Sources(
                levelAuthority = "Caller-supplied level-tagged wordlist (levels are approximate, not official CEFR)",
                enrichment = "Kaikki/Wiktextract German extract (CC BY-SA 4.0)",
                tatoeba = if (tatoebaIndex != null) "Tatoeba deu-eng sentence pairs (CC BY 2.0 FR)" else null,
                odenet = if (odenetByLemma != null) "OdeNet — Open German WordNet relations (CC BY-SA 4.0)" else null,
                simpleCase = if (simpleCaseByLemma != null) {
                    "Tatoeba-derived simple case examples (CC BY 2.0 FR)"
                } else null,
                ud = if (udByLemma != null) {
                    "Universal Dependencies German GSD treebank (CC BY-SA 4.0; fallback for case examples)"
                } else null,
            ),
            licenses = licenses,
            tool = Tool(name = "dataset-builder", version = toolVersion),
        )

        val qa = QaReport(
            generatedAt = generatedAt,
            datasetVersion = config.datasetVersion,
            wordlistRows = rows.size,
            emittedEntries = sortedEntries.size,
            matched = matched,
            unmatched = unmatched.size,
            matchRate = matchRate,
            levels = orderedLevels,
            perLevel = perLevel,
            missingFields = DatasetWriter.fieldCoverage(sortedEntries),
            kaikkiLinesRead = streamStats.linesRead,
            kaikkiLinesSkipped = streamStats.linesSkipped,
            idCollisions = idCollisions,
            unmatchedLemmas = unmatched,
            notes = buildList {
                add("Committed manually from the official Wortliste PDFs; PDFs are never read here.")
                add("Enrichment fields absent in Kaikki are left null (never fabricated).")
                if (streamStats.linesSkipped > 0) {
                    add("${streamStats.linesSkipped} Kaikki lines were unparseable and skipped.")
                }
                addAll(notes)
            },
        )

        val written = DatasetWriter.write(config.outputDir, config.datasetVersion, dataset, manifest, qa)
        return PipelineResult(sortedEntries, manifest, qa, written)
    }

    /** Records a lemma and every inflected form Kaikki listed under a normalized surface key. */
    private fun registerForms(into: HashMap<String, MutableSet<String>>, lemma: String, kaikki: CompactKaikki?) {
        val normLemma = normalizeLemma(lemma)
        into.getOrPut(normLemma) { mutableSetOf() }.add(normLemma)
        for (form in kaikki?.forms.orEmpty()) {
            val key = normalizeLemma(form)
            if (key.isNotEmpty()) into.getOrPut(key) { mutableSetOf() }.add(normLemma)
        }
    }

    /**
     * Loads an optional source, degrading gracefully: a null path, an unreadable file or any parse
     * error results in a QA note and a null result — the pipeline never fails because of an optional
     * enrichment input.
     */
    private fun <T> tryLoad(
        path: Path?,
        name: String,
        notes: MutableList<String>,
        load: (Path) -> T,
    ): T? {
        if (path == null) return null
        return try {
            if (!Files.isReadable(path)) {
                notes.add("$name input not readable; skipped: $path")
                null
            } else {
                load(path)
            }
        } catch (e: Exception) {
            notes.add("$name input failed to load; skipped: ${e.message}")
            null
        }
    }
}
