package app.vocabflash.datasetbuilder.wordlist

import app.vocabflash.datasetbuilder.util.normalizeGender
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * A row of the manually-prepared wordlist file.
 *
 * The file is supplied by the caller; the repository ships none. See `README.md`
 * ("Preparing the wordlist input") for the expected format and open-source examples.
 */
data class WordListRow(
    val lemma: String,
    val level: String,
    val pos: String? = null,
    val article: String? = null,
    val gender: String? = null,
    val plural: String? = null,
    val praeteritum: String? = null,
    val partizipII: String? = null,
    val separable: Boolean? = null,
    val translations: List<String> = emptyList(),
    val topic: String? = null,
)

/**
 * Reads the normalized wordlist (TSV or CSV) produced by hand from the official Wortliste.
 *
 * Rules:
 * - The first non-blank line is a header; column names are matched case-insensitively and a few
 *   common synonyms are accepted (see [COLUMN_ALIASES]).
 * - The delimiter is auto-detected: tab if the header contains a tab, otherwise comma.
 * - Only `lemma` and `level` are required; every other column is optional.
 * - Multiple translations are separated by `;` or `|`.
 * - Rows whose level is not in [allowedLevels] are dropped (level filtering is data, per D26).
 */
object WordListSource {

    private val COLUMN_ALIASES: Map<String, Set<String>> = mapOf(
        "lemma" to setOf("lemma", "word", "stichwort", "wort", "headword"),
        "level" to setOf("level", "niveau", "cefr", "stufe"),
        "pos" to setOf("pos", "wortart", "partofspeech", "part_of_speech", "type"),
        "article" to setOf("article", "artikel", "art"),
        "gender" to setOf("gender", "genus", "geschlecht"),
        "plural" to setOf("plural", "pluralform", "mehrzahl"),
        "praeteritum" to setOf("praeteritum", "präteritum", "preterite", "past", "verb_past"),
        "partizipII" to setOf("partizip2", "partizipii", "partizip_ii", "participle", "past_participle", "verb_pp"),
        "separable" to setOf("separable", "trennbar", "verb_separable"),
        "translation" to setOf("translation", "translations", "uebersetzung", "übersetzung", "english", "bedeutung"),
        "topic" to setOf("topic", "thema", "category", "tag"),
    )

    fun read(path: Path, allowedLevels: Set<String>): List<WordListRow> {
        val text = path.readText()
        val lines = text.split('\n')
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "wordlist file is empty: $path" }

        val delimiter = if (lines.first().contains('\t')) '\t' else ','
        val header = parseDelimited(lines.first(), delimiter).map { it.trim().lowercase() }
        val colIndex = resolveColumns(header)

        val lemmaIdx = colIndex["lemma"]
            ?: error("wordlist header must contain a 'lemma' column (got: $header)")
        val levelIdx = colIndex["level"]
            ?: error("wordlist header must contain a 'level' column (got: $header)")

        val rows = ArrayList<WordListRow>()
        for (raw in lines.drop(1)) {
            val cells = parseDelimited(raw, delimiter)
            fun cell(name: String): String? =
                colIndex[name]?.let { i -> cells.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() } }

            val lemma = cells.getOrNull(lemmaIdx)?.trim().orEmpty()
            if (lemma.isEmpty()) continue
            val level = cells.getOrNull(levelIdx)?.trim().orEmpty().uppercase()
            if (level.isEmpty() || level !in allowedLevels) continue

            rows.add(
                WordListRow(
                    lemma = lemma,
                    level = level,
                    pos = cell("pos")?.lowercase(),
                    article = cell("article")?.lowercase(),
                    gender = normalizeGender(cell("gender")),
                    plural = cell("plural"),
                    praeteritum = cell("praeteritum"),
                    partizipII = cell("partizipII"),
                    separable = cell("separable")?.toBooleanStrictOrNull(),
                    translations = cell("translation").splitTranslations(),
                    topic = cell("topic"),
                ),
            )
        }
        return rows
    }

    private fun resolveColumns(header: List<String>): Map<String, Int> {
        val out = HashMap<String, Int>()
        for ((canonical, aliases) in COLUMN_ALIASES) {
            val idx = header.indexOfFirst { it in aliases }
            if (idx >= 0) out[canonical] = idx
        }
        return out
    }
}

private fun String?.splitTranslations(): List<String> =
    this?.split(';', '|')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        .orEmpty()

/**
 * Minimal RFC-4180-ish reader: handles `"`-quoted fields and doubled `""` escapes inside quotes.
 * Keeps commas/tabs inside quotes intact. Not a full CSV library on purpose (zero dependencies).
 */
internal fun parseDelimited(line: String, delimiter: Char): List<String> {
    val out = ArrayList<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> sb.append(c)
            }
            c == '"' -> inQuotes = true
            c == delimiter -> { out.add(sb.toString()); sb.clear() }
            else -> sb.append(c)
        }
        i++
    }
    out.add(sb.toString())
    return out
}
