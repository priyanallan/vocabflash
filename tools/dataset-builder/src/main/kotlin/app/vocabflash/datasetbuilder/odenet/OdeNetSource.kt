package app.vocabflash.datasetbuilder.odenet

import app.vocabflash.datasetbuilder.model.Related
import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Reads a **prepared** OdeNet relation export and indexes it by lemma.
 *
 * ## Source & licence
 *
 * OdeNet — *Open German WordNet* — is the German WordNet, distributed by the
 * [Hochschule Hannover / IDS](https://github.com/hnnydx/OdeNet) as **LMF XML** under
 * **CC BY-SA 4.0**. See the module README for how to obtain the dump.
 *
 * ## Accepted input format (documented choice)
 *
 * Parsing the multi-hundred-MB LMF XML directly is out of scope. Instead this reader accepts a flat
 * **prepared TSV export with a header `lemma<TAB>relation<TAB>related`**, one relation per line,
 * where `relation` is one of:
 *
 * | export token | meaning              | emitted as |
 * | ---          | ---                  | ---        |
 * | `broader`    | hypernym             | `broader`  |
 * | `narrower`   | hyponym              | `narrower` |
 * | `similar`    | co-hyponym/synonym   | `similar`  |
 *
 * Common LMF/WordNet spellings are accepted as aliases (`hypernym`/`hyperonym`, `hyponym`,
 * `synonym`, `co-hyponym`, `also`). The README shows how to flatten the real OdeNet LMF into this
 * TSV. This keeps the tool dependency-light and the fixture tiny.
 */
object OdeNetSource {

    private val RELATION_ALIASES: Map<String, String> = mapOf(
        "broader" to "broader", "hypernym" to "broader", "hyperonym" to "broader",
        "narrower" to "narrower", "hyponym" to "narrower",
        "similar" to "similar", "synonym" to "similar", "co-hyponym" to "similar", "also" to "similar",
    )

    /** Reads the prepared relation TSV. Missing/unreadable files throw (the caller degrades). */
    fun read(path: Path): Map<String, List<Related>> {
        if (!Files.isReadable(path)) error("OdeNet file not readable: $path")
        val lines = path.readText().split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyMap()

        val header = lines.first().split('\t').map { it.trim().lowercase() }
        val lemmaCol = header.indexOfFirst { it == "lemma" || it == "word" || it == "source" }
        val relCol = header.indexOfFirst { it == "relation" || it == "reltype" || it == "rel" }
        val relToCol = header.indexOfFirst { it == "related" || it == "target" || it == "word2" }
        // Fall back to positional columns when there is no recognisable header.
        val hasHeader = lemmaCol >= 0 && relCol >= 0 && relToCol >= 0
        val lc = if (hasHeader) lemmaCol else 0
        val rc = if (hasHeader) relCol else 1
        val ttc = if (hasHeader) relToCol else 2

        val out = LinkedHashMap<String, MutableList<Related>>()
        val body = if (hasHeader) lines.drop(1) else lines
        for (raw in body) {
            val cols = raw.split('\t')
            val lemma = cols.getOrNull(lc)?.trim().orEmpty()
            val relRaw = cols.getOrNull(rc)?.trim()?.lowercase().orEmpty()
            val target = cols.getOrNull(ttc)?.trim().orEmpty()
            if (lemma.isEmpty() || target.isEmpty()) continue
            val relation = RELATION_ALIASES[relRaw] ?: continue
            if (target.equals(lemma, ignoreCase = true)) continue
            val list = out.getOrPut(normalizeLemma(lemma)) { ArrayList() }
            if (list.none { it.word.equals(target, ignoreCase = true) && it.relation == relation }) {
                list.add(Related(word = target, relation = relation))
            }
        }
        return out
    }
}
