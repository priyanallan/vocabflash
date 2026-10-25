package app.vocabflash.datasetbuilder.kaikki

import app.vocabflash.datasetbuilder.model.Example

/**
 * The trimmed slice of a Kaikki/Wiktextract (CC BY-SA 4.0) line that the pipeline actually needs.
 *
 * Kaikki emits one JSON object per word *sense*; a single lemma usually appears on several lines
 * (one per POS/sense). The matcher keeps the richest [CompactKaikki] per lemma.
 */
data class CompactKaikki(
    val word: String,
    val pos: String? = null,
    val gender: String? = null,
    val plural: String? = null,
    val infinitive: String? = null,
    val praeteritum: String? = null,
    val partizipII: String? = null,
    val partizipI: String? = null,
    val presentDu: String? = null,
    val presentEr: String? = null,
    val presentIhr: String? = null,
    val translations: List<String> = emptyList(),
    val ipa: List<String> = emptyList(),
    val examples: List<Example> = emptyList(),
    /**
     * Every surface form string Kaikki lists for this sense (`forms[].form`), deduplicated. Used to
     * widen the search keys when matching Tatoeba sentences to a lemma (inflected forms).
     */
    val forms: List<String> = emptyList(),
) {
    /** Rough "is this the better of two same-lemma entries?" heuristic. */
    fun richness(): Int =
        (if (gender != null) 4 else 0) +
            (if (plural != null) 3 else 0) +
            (if (praeteritum != null || partizipII != null) 3 else 0) +
            (if (translations.isNotEmpty()) 2 else 0) +
            (if (ipa.isNotEmpty()) 1 else 0) +
            (if (examples.isNotEmpty()) 1 else 0)
}
