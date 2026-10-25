package app.vocabflash.datasetbuilder.enrich

import app.vocabflash.datasetbuilder.wordlist.WordListRow
import app.vocabflash.datasetbuilder.kaikki.CompactKaikki
import app.vocabflash.datasetbuilder.model.CaseUsage
import app.vocabflash.datasetbuilder.model.DatasetEntry
import app.vocabflash.datasetbuilder.model.EntrySources
import app.vocabflash.datasetbuilder.model.Example
import app.vocabflash.datasetbuilder.model.Related
import app.vocabflash.datasetbuilder.model.VerbForms
import app.vocabflash.datasetbuilder.util.articleForGender
import app.vocabflash.datasetbuilder.util.normalizeLemma
import app.vocabflash.datasetbuilder.util.stableId

/** Caps applied while merging enrichment so the bundled Dataset stays small. */
data class EnrichLimits(
    val maxTranslations: Int = 5,
    val maxExamples: Int = 3,
    val maxIpa: Int = 3,
    /** Cap on Tatoeba example sentences merged per entry (diversity-selected). */
    val maxTatoebaExamples: Int = 5,
    /** Cap on OdeNet `related` entries merged per entry. */
    val maxRelated: Int = 6,
)

/**
 * Merges a wordlist level-authority row with its (optional) enrichment sources into a [DatasetEntry].
 *
 * Precedence: the supplied wordlist is authoritative for lemma/level/pos and any field it provides.
 * Kaikki only *fills gaps* (article/gender/plural, verb principal parts, extra translations,
 * IPA, examples). Article is derived from gender when the source omits it. The optional Tatoeba,
 * OdeNet and UD sources are merged afterwards by the [Pipeline] via [withTatoeba] / [withRelated] /
 * [withCaseUsage].
 */
object Enricher {

    fun enrich(row: WordListRow, kaikki: CompactKaikki?, limits: EnrichLimits): DatasetEntry {
        val gender = row.gender ?: kaikki?.gender
        val article = row.article ?: articleForGender(gender)

        val translations = LinkedHashSet<String>().apply {
            addAll(row.translations)
            kaikki?.translations?.forEach { add(it) }
        }.take(limits.maxTranslations)

        val verb = if (
            row.pos == "verb" || row.praeteritum != null || row.partizipII != null ||
            kaikki?.praeteritum != null || kaikki?.partizipII != null || kaikki?.infinitive != null
        ) {
            VerbForms(
                infinitive = kaikki?.infinitive ?: row.lemma,
                praeteritum = row.praeteritum ?: kaikki?.praeteritum,
                partizipII = row.partizipII ?: kaikki?.partizipII,
                partizipI = kaikki?.partizipI,
                presentDu = kaikki?.presentDu,
                presentEr = kaikki?.presentEr,
                presentIhr = kaikki?.presentIhr,
                separable = row.separable,
            )
        } else {
            null
        }

        val entry = DatasetEntry(
            id = stableId(row.level, row.lemma, row.pos),
            lemma = row.lemma,
            normalizedLemma = normalizeLemma(row.lemma),
            level = row.level,
            pos = row.pos,
            article = article,
            gender = gender,
            plural = row.plural ?: kaikki?.plural,
            verb = verb,
            translations = translations,
            ipa = kaikki?.ipa?.take(limits.maxIpa).orEmpty(),
            examples = kaikki?.examples?.take(limits.maxExamples).orEmpty()
                .map { Example(it.de, it.en, source = "kaikki") },
            topic = row.topic,
            sources = EntrySources(wordlist = true, kaikki = kaikki != null),
        )
        return entry
    }

    /**
     * Merges diversity-selected Tatoeba sentences after the Kaikki examples (Kaikki first), removing
     * duplicates by normalized German text. Sets the `tatoeba` provenance flag when anything merged.
     */
    fun withTatoeba(entry: DatasetEntry, tatoeba: List<Example>): DatasetEntry {
        if (tatoeba.isEmpty()) return entry
        val merged = mergeExamples(entry.examples, tatoeba)
        return entry.copy(examples = merged, sources = entry.sources.copy(tatoeba = true))
    }

    /** Merges OdeNet `related` entries, deduplicated and excluding the entry's own lemma. */
    fun withRelated(entry: DatasetEntry, related: List<Related>, max: Int): DatasetEntry {
        val filtered = related
            .filter { it.word.isNotBlank() && !it.word.equals(entry.lemma, ignoreCase = true) }
            .distinctBy { it.word.lowercase() to it.relation }
            .take(max)
        if (filtered.isEmpty()) return entry
        return entry.copy(related = filtered, sources = entry.sources.copy(odenet = true))
    }

    /** Merges UD case evidence (at most one per grammatical case), keeping only known cases. */
    fun withCaseUsage(entry: DatasetEntry, caseUsage: List<CaseUsage>): DatasetEntry {
        val filtered = caseUsage
            .filter { it.case == "accusative" || it.case == "dative" }
            .distinctBy { it.case }
            .sortedBy { it.case }
        if (filtered.isEmpty()) return entry
        return entry.copy(caseUsage = filtered, sources = entry.sources.copy(ud = true))
    }

    /**
     * Kaikki first, then the extra source; duplicates (by normalized German text) keep the first
     * occurrence so Kaikki provenance is preserved.
     */
    private fun mergeExamples(primary: List<Example>, extra: List<Example>): List<Example> {
        val out = ArrayList<Example>(primary.size + extra.size)
        val seen = HashSet<String>()
        for (e in primary + extra) {
            if (seen.add(normalizeExample(e.de))) out.add(e)
        }
        return out
    }
}

/** Whitespace/punctuation-insensitive key used only for de-duplicating example sentences. */
internal fun normalizeExample(s: String): String =
    s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
