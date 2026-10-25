package app.vocabflash.datasetbuilder.ud

import app.vocabflash.datasetbuilder.model.CaseUsage
import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/** One token line of a CoNLL-U file (only the fields this tool needs). */
internal data class UdToken(
    val id: Int,
    val form: String,
    val lemma: String,
    val upos: String,
    val feats: Map<String, String>,
    val head: Int,
    val deprel: String,
)

/** One parsed CoNLL-U sentence. */
internal data class UdSentence(val text: String, val tokens: List<UdToken>)

/**
 * Reads **Universal Dependencies German GSD** CoNLL-U files (CC BY-SA 4.0) and derives case-governance
 * evidence (`caseUsage`) for wordlist nouns.
 *
 * ## Method
 *
 * For each sentence, every `NOUN`/`PROPN` token whose `LEMMA` matches a wordlist lemma
 * (case-insensitively) is inspected:
 * - its `Case` feature (`Acc`, `Dat`) selects the emitted `case`;
 * - the sentence text is taken from the `# text = …` comment when present, else reconstructed by
 *   joining the surface forms;
 * - the gloss is generated: if a `case`-relation child points at the noun (a preposition/adposition),
 *   e.g. `mit`, the gloss is `After "mit"`; otherwise `Direct object` (accusative) or
 *   `Indirect object` (dative).
 *
 * At most **one** evidence item per case (accusative, dative) per lemma is retained, and lemmas with
 * no UD evidence are simply absent — no case is ever guessed.
 */
object UdCaseSource {

    /** Loads and indexes a CoNLL-U file. Missing/unreadable files throw (the caller degrades). */
    fun read(path: Path): Map<String, List<CaseUsage>> {
        if (!Files.isReadable(path)) error("UD file not readable: $path")
        val sentences = parse(path.readText())
        val byLemma = HashMap<String, MutableMap<String, CaseUsage>>()
        for (sentence in sentences) {
            for (token in sentence.tokens) {
                if (token.upos != "NOUN" && token.upos != "PROPN") continue
                if (token.lemma.isEmpty() || token.lemma == "_") continue
                val case = caseOf(token.feats) ?: continue
                val gloss = glossFor(token, sentence) ?: continue
                val entry = CaseUsage(case = case, sentence = sentence.text, gloss = gloss)
                val perLemma = byLemma.getOrPut(normalizeLemma(token.lemma)) { HashMap() }
                perLemma.putIfAbsent(case, entry) // first occurrence per case wins
            }
        }
        return byLemma.mapValues { (_, byCase) -> byCase.values.sortedBy { it.case } }
    }

    private fun caseOf(feats: Map<String, String>): String? = when (feats["Case"]) {
        "Acc" -> "accusative"
        "Dat" -> "dative"
        else -> null
    }

    private fun glossFor(token: UdToken, sentence: UdSentence): String? {
        // A `case`-relation dependent of the noun is a preposition/adposition governing it.
        val preposition = sentence.tokens.firstOrNull { it.head == token.id && it.deprel == "case" }?.form
        if (preposition != null && preposition != "_") return "After \"$preposition\""
        return when (caseOf(token.feats)) {
            "accusative" -> "Direct object"
            "dative" -> "Indirect object"
            else -> null
        }
    }

    /** Minimal CoNLL-U reader: blank-line-separated sentences, `#` comments, 10 tab-separated columns. */
    internal fun parse(text: String): List<UdSentence> {
        val sentences = ArrayList<UdSentence>()
        var currentText: String? = null
        val currentTokens = ArrayList<UdToken>()

        fun flush() {
            if (currentTokens.isNotEmpty()) {
                val body = currentText ?: currentTokens.joinToString(" ") { it.form }
                sentences.add(UdSentence(body, currentTokens.toList()))
            }
            currentTokens.clear()
            currentText = null
        }

        for (raw in text.split('\n')) {
            val line = raw.trimEnd('\r')
            if (line.isBlank()) { flush(); continue }
            if (line.startsWith("#")) {
                val m = Regex("^#\\s*text\\s*=\\s*(.*)$").find(line)
                if (m != null) currentText = m.groupValues[1].trim()
                continue
            }
            val cols = line.split('\t')
            if (cols.size < 8) continue
            val id = cols[0].toIntOrNull() ?: continue // skip multiword-token ranges (1-2) and empty nodes
            currentTokens.add(
                UdToken(
                    id = id,
                    form = cols[1],
                    lemma = cols[2],
                    upos = cols[3],
                    feats = parseFeats(cols[5]),
                    head = cols[6].toIntOrNull() ?: 0,
                    deprel = cols[7].substringBefore(':'),
                ),
            )
        }
        flush()
        return sentences
    }

    private fun parseFeats(raw: String): Map<String, String> {
        if (raw == "_" || raw.isEmpty()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
        }.toMap()
    }
}
