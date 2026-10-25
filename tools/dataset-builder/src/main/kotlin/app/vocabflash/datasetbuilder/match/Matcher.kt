package app.vocabflash.datasetbuilder.match

import app.vocabflash.datasetbuilder.wordlist.WordListRow
import app.vocabflash.datasetbuilder.kaikki.CompactKaikki
import app.vocabflash.datasetbuilder.util.normalizeLemma

/** Result of matching one wordlist row against the Kaikki index. */
sealed interface MatchResult {
    data class Matched(val entry: CompactKaikki) : MatchResult
    data class Unmatched(val lemma: String, val level: String) : MatchResult
}

/**
 * Matches wordlist lemmas against Kaikki entries.
 *
 * Strategy (in order): exact case-sensitive lemma, then case-normalized lemma. Ambiguous POS is not
 * resolved here — the wordlist's `pos` column is the authority when present, and Kaikki's
 * richest entry wins ties.
 */
class Matcher(private val index: KaikkiIndexLookup) {

    fun match(row: WordListRow): MatchResult {
        val hit = index.lookup(row.lemma)
        return if (hit != null) MatchResult.Matched(hit) else MatchResult.Unmatched(row.lemma, row.level)
    }

    fun normalizedLemmaOf(row: WordListRow): String = normalizeLemma(row.lemma)
}

/** Narrow view of the Kaikki index the matcher depends on (keeps the matcher unit-testable). */
fun interface KaikkiIndexLookup {
    fun lookup(lemma: String): CompactKaikki?
}
