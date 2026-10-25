package app.vocabflash.datasetbuilder.tatoeba

import app.vocabflash.datasetbuilder.model.CaseUsage
import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Reads the prepared, **learner-friendly** case-example TSV produced by `tatoeba_case_examples.py`
 * and indexes it by normalized lemma.
 *
 * ## Why this exists
 *
 * The UD German GSD treebank ([app.vocabflash.datasetbuilder.ud.UdCaseSource]) yields *corpus*
 * sentences, which are frequently long and syntactically complex — poor A1/A2 material. This source
 * instead reads Tatoeba sentences that a small script already filtered to be short and to carry an
 * **unambiguous** case marker (a governing preposition), so the examples are comprehensible at a low
 * level. Because it produces the same `Map<String, List<CaseUsage>>` shape as the UD source, it flows
 * through `Enricher.withCaseUsage` unchanged.
 *
 * ## Format (tab-separated, one example per line)
 *
 * ```
 * <lemma>\t<case>\t<sentence>\t<gloss>
 * Brot\taccusative\tIss das Brot!\tAfter "für"
 * ```
 *
 * `case` is `accusative` or `dative`; `gloss` is optional (absent ⇒ empty string). Only those two
 * case values are accepted, so a header row or a malformed line is simply skipped. Blank lines and
 * `#` comment lines are ignored. At most **one** evidence item per case per lemma is kept (the first
 * occurrence wins) and lemmas are keyed case-insensitively via
 * [normalizeLemma][app.vocabflash.datasetbuilder.util.normalizeLemma].
 *
 * Licence: Tatoeba sentences are CC BY 2.0 FR — attribution required.
 */
object SimpleCaseSource {

    private const val ACCUSATIVE = "accusative"
    private const val DATIVE = "dative"

    /** Loads the prepared TSV. Missing/unreadable files throw (the caller degrades). */
    fun read(path: Path): Map<String, List<CaseUsage>> {
        if (!Files.isReadable(path)) error("Simple-case file not readable: $path")
        val byLemma = HashMap<String, MutableMap<String, CaseUsage>>()
        for (raw in path.readText().split('\n')) {
            val line = raw.trimEnd('\r')
            if (line.isBlank() || line.startsWith("#")) continue
            val cols = line.split('\t')
            if (cols.size < 3) continue
            val lemma = cols[0].trim()
            val case = cols[1].trim().lowercase()
            val sentence = cols[2].trim()
            if (lemma.isEmpty() || sentence.isEmpty()) continue
            if (case != ACCUSATIVE && case != DATIVE) continue // skips headers and stray lines
            val gloss = cols.getOrNull(3)?.trim().orEmpty()
            val perLemma = byLemma.getOrPut(normalizeLemma(lemma)) { HashMap() }
            perLemma.putIfAbsent(case, CaseUsage(case = case, sentence = sentence, gloss = gloss))
        }
        return byLemma.mapValues { (_, byCase) -> byCase.values.sortedBy { it.case } }
    }
}
