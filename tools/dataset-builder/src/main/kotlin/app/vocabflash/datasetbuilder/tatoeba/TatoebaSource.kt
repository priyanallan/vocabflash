package app.vocabflash.datasetbuilder.tatoeba

import app.vocabflash.datasetbuilder.model.Example
import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * A German↔English sentence pair from a Tatoeba export (CC BY 2.0 FR).
 */
data class TatoebaPair(val de: String, val en: String?)

/**
 * One candidate sentence found for a wordlist lemma, carrying the surface form that matched and the
 * token that immediately precedes it — both used to prefer *context diversity*.
 */
data class TatoebaCandidate(
    val de: String,
    val en: String?,
    val matchedForm: String,
    val precedingToken: String?,
)

/**
 * Reads a **prepared** Tatoeba sentence-pair export and indexes it by the surface forms we care
 * about.
 *
 * ## Accepted input format (documented choice)
 *
 * We support one format: a **TSV with a header row `de<TAB>en`**, one sentence pair per line.
 * German is the first column (required), English the second (optional). This is the "simplified
 * prepared TSV" the real `deu-eng` links file + `deu_sentences.tsv` + `eng_sentences.tsv` are joined
 * into once, outside this tool (see the module README for a one-liner). Reading the raw Tatoeba
 * links/sentences triple is deliberately *not* implemented here: joining three ~tens-of-MB files is
 * better done once with `awk`/`python` and committed to a local, uncommitted working file.
 *
 * ## Matching
 *
 * Sentences are indexed by their tokens. The caller supplies a `form → lemmas` map (built from each
 * wordlist lemma plus every inflected form Kaikki lists for it), so sentences containing inflected
 * forms (`läuft`, `Häuser`) are found too.
 */
object TatoebaSource {

    /** Reads the prepared `de<TAB>en` TSV. Missing/unreadable files throw (the caller degrades). */
    fun read(path: Path): List<TatoebaPair> {
        if (!Files.isReadable(path)) error("Tatoeba file not readable: $path")
        val lines = path.readText().split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().split('\t').map { it.trim().lowercase() }
        val deIdx = header.indexOfFirst { it == "de" || it == "german" || it == "deu" }
        val enIdx = header.indexOfFirst { it == "en" || it == "english" || it == "eng" }
        val deCol = if (deIdx >= 0) deIdx else 0
        val enCol = if (enIdx >= 0) enIdx else if (deCol == 0) 1 else -1

        val pairs = ArrayList<TatoebaPair>(lines.size - 1)
        for (raw in lines.drop(1)) {
            val cols = raw.split('\t')
            val de = cols.getOrNull(deCol)?.trim().orEmpty()
            if (de.isEmpty()) continue
            val en = if (enCol >= 0) cols.getOrNull(enCol)?.trim()?.takeIf { it.isNotEmpty() } else null
            pairs.add(TatoebaPair(de, en))
        }
        return pairs
    }

    /** Lowercased letter runs; umlauts/ß preserved. Used for token matching and diversity keys. */
    fun tokenize(sentence: String): List<String> =
        sentence.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

    /** Builds a per-lemma index from sentence pairs and a `surface form → lemmas` map. */
    fun build(pairs: List<TatoebaPair>, formToLemmas: Map<String, Set<String>>): TatoebaIndex {
        val byLemma = HashMap<String, MutableList<TatoebaCandidate>>()
        for (pair in pairs) {
            val tokens = tokenize(pair.de)
            for ((i, token) in tokens.withIndex()) {
                val lemmas = formToLemmas[token] ?: continue
                val prev = if (i > 0) tokens[i - 1] else null
                for (lemma in lemmas) {
                    byLemma.getOrPut(lemma) { ArrayList() }
                        .add(TatoebaCandidate(pair.de, pair.en, matchedForm = token, precedingToken = prev))
                }
            }
        }
        return TatoebaIndex(byLemma)
    }
}

/**
 * A read-only index of Tatoeba candidates keyed by normalized wordlist lemma. Callers pick a bounded,
 * diversity-ranked slice with [select].
 */
class TatoebaIndex internal constructor(
    private val byLemma: Map<String, List<TatoebaCandidate>>,
) {
    fun candidates(lemma: String): List<TatoebaCandidate> = byLemma[normalizeLemma(lemma)].orEmpty()

    /**
     * Selects up to [max] examples for [lemma], preferring context diversity:
     * 1. different inflected forms of the word,
     * 2. different preceding tokens (article/preposition context),
     * 3. original order otherwise.
     *
     * Near-duplicate sentences (same letters/digits) are collapsed first.
     */
    fun select(lemma: String, max: Int): List<Example> {
        if (max <= 0) return emptyList()
        val unique = LinkedHashMap<String, TatoebaCandidate>()
        for (c in candidates(lemma)) unique.putIfAbsent(exampleKey(c.de), c)
        val pool = unique.values.toMutableList()
        val selected = ArrayList<TatoebaCandidate>(max)
        val seenForms = HashSet<String>()
        val seenPreceding = HashSet<String>()
        while (selected.size < max && pool.isNotEmpty()) {
            val pick = pool.firstOrNull { it.matchedForm !in seenForms }
                ?: pool.firstOrNull { (it.precedingToken ?: "") !in seenPreceding }
                ?: pool.first()
            selected.add(pick)
            pool.remove(pick)
            seenForms.add(pick.matchedForm)
            seenPreceding.add(pick.precedingToken ?: "")
        }
        return selected.map { Example(de = it.de, en = it.en, source = "tatoeba") }
    }
}

/** Letters/digits-only key used only to collapse near-duplicate sentences. */
internal fun exampleKey(s: String): String =
    s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
