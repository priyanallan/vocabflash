package app.vocabflash.datasetbuilder.emit

import app.vocabflash.datasetbuilder.model.DatasetFile
import app.vocabflash.datasetbuilder.model.DatasetEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/** SCHEMA_VERSION of the emitted Dataset JSON. Bump only on breaking shape changes. */
const val SCHEMA_VERSION: Int = 1

/** Machine-readable build manifest emitted alongside the Dataset. */
@Serializable
data class Manifest(
    val schemaVersion: Int,
    val datasetVersion: String,
    val generatedAt: String,
    val levels: List<String>,
    val entryCount: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val matchRate: Double,
    val perLevel: Map<String, Int>,
    val sources: Sources,
    val licenses: List<String>,
    val tool: Tool,
)

@Serializable
data class Sources(
    val levelAuthority: String,
    val enrichment: String,
    /** Optional additional enrichment sources; null/omitted when the source was not supplied. */
    val tatoeba: String? = null,
    val odenet: String? = null,
    /** Tatoeba-derived simple case examples; preferred over [ud] when both are supplied. */
    val simpleCase: String? = null,
    val ud: String? = null,
)

@Serializable
data class Tool(
    val name: String,
    val version: String,
)

/** QA summary — also rendered to human-readable Markdown by [DatasetWriter]. */
@Serializable
data class QaReport(
    val generatedAt: String,
    val datasetVersion: String,
    val wordlistRows: Int,
    val emittedEntries: Int,
    val matched: Int,
    val unmatched: Int,
    val matchRate: Double,
    val levels: List<String>,
    val perLevel: Map<String, Int>,
    val missingFields: Map<String, Int>,
    val kaikkiLinesRead: Long,
    val kaikkiLinesSkipped: Long,
    val idCollisions: List<String>,
    val unmatchedLemmas: List<UnmatchedLemma>,
    val notes: List<String>,
)

@Serializable
data class UnmatchedLemma(val level: String, val lemma: String)

/**
 * Writes the Dataset, manifest, QA report and unmatched list into [outputDir].
 *
 * All output is plain JSON/TSV so a downstream step can bundle or post-process it without this tool.
 */
object DatasetWriter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun write(
        outputDir: Path,
        datasetVersion: String,
        dataset: DatasetFile,
        manifest: Manifest,
        qa: QaReport,
    ): List<Path> {
        Files.createDirectories(outputDir)
        val written = ArrayList<Path>()

        outputDir.resolve("dataset.json").let { it.writeText(json.encodeToString(dataset)); written.add(it) }
        outputDir.resolve("manifest.json").let { it.writeText(json.encodeToString(manifest)); written.add(it) }
        outputDir.resolve("qa-report.md").let { it.writeText(renderQa(qa)); written.add(it) }
        outputDir.resolve("unmatched.tsv").let {
            it.writeText(renderUnmatched(qa)); written.add(it)
        }
        return written
    }

    private fun renderUnmatched(qa: QaReport): String = buildString {
        appendLine("level\tlemma")
        qa.unmatchedLemmas
            .sortedWith(compareBy({ it.level }, { it.lemma }))
            .forEach { appendLine("${it.level}\t${it.lemma}") }
    }

    private fun renderQa(qa: QaReport): String = buildString {
        appendLine("# Dataset QA report")
        appendLine()
        appendLine("- Dataset version: `${qa.datasetVersion}`")
        appendLine("- Generated at: ${qa.generatedAt}")
        appendLine("- Levels: ${qa.levels.joinToString(", ")}")
        appendLine()
        appendLine("## Totals")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("| --- | --- |")
        appendLine("| Wordlist rows read | ${qa.wordlistRows} |")
        appendLine("| Entries emitted | ${qa.emittedEntries} |")
        appendLine("| Matched in Kaikki | ${qa.matched} |")
        appendLine("| Unmatched | ${qa.unmatched} |")
        appendLine("| Match rate | ${"%.1f".format(qa.matchRate * 100)}% |")
        appendLine("| Kaikki lines read | ${qa.kaikkiLinesRead} |")
        appendLine("| Kaikki lines skipped | ${qa.kaikkiLinesSkipped} |")
        appendLine()
        appendLine("## Entries per level")
        appendLine()
        appendLine("| Level | Entries |")
        appendLine("| --- | --- |")
        qa.perLevel.toSortedMap().forEach { (lvl, n) -> appendLine("| $lvl | $n |") }
        appendLine()
        appendLine("## Field coverage (entries missing a value)")
        appendLine()
        appendLine("| Field | Missing |")
        appendLine("| --- | --- |")
        qa.missingFields.toSortedMap().forEach { (field, n) -> appendLine("| $field | $n |") }
        appendLine()
        if (qa.idCollisions.isNotEmpty()) {
            appendLine("## ⚠️ ID collisions")
            appendLine()
            qa.idCollisions.forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("## Notes")
        appendLine()
        qa.notes.forEach { appendLine("- $it") }
        appendLine()
        if (qa.unmatchedLemmas.isNotEmpty()) {
            appendLine("## Unmatched wordlist lemmas (${qa.unmatchedLemmas.size})")
            appendLine()
            appendLine("These entries were emitted with wordlist-only data. See `unmatched.tsv`.")
            appendLine()
            qa.unmatchedLemmas
                .sortedWith(compareBy({ it.level }, { it.lemma }))
                .take(200)
                .forEach { appendLine("- ${it.level}: ${it.lemma}") }
            if (qa.unmatchedLemmas.size > 200) appendLine("- … (${qa.unmatchedLemmas.size - 200} more)")
        }
    }

    /** Shared post-build checks that populate [QaReport.missingFields]. */
    fun fieldCoverage(entries: List<DatasetEntry>): Map<String, Int> {
        val verbs = entries.filter { it.verb != null }
        return mapOf(
            "article" to entries.count { it.article.isNullOrBlank() },
            "gender" to entries.count { it.gender.isNullOrBlank() },
            "plural" to entries.count { it.plural.isNullOrBlank() },
            "translations" to entries.count { it.translations.isEmpty() },
            "ipa" to entries.count { it.ipa.isEmpty() },
            "examples" to entries.count { it.examples.isEmpty() },
            // Optional enrichment sources (empty means the source was absent or had no evidence).
            "related" to entries.count { it.related.isEmpty() },
            "caseUsage" to entries.count { it.caseUsage.isEmpty() },
            // Verb fields are reported against verb entries only (non-verbs would inflate the count).
            "verb.presentDu" to verbs.count { it.verb?.presentDu.isNullOrBlank() },
            "verb.presentEr" to verbs.count { it.verb?.presentEr.isNullOrBlank() },
            "verb.presentIhr" to verbs.count { it.verb?.presentIhr.isNullOrBlank() },
            "verb.partizipI" to verbs.count { it.verb?.partizipI.isNullOrBlank() },
        )
    }
}
