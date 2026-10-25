package app.vocabflash.datasetbuilder.model

import kotlinx.serialization.Serializable

/**
 * The bundled, versioned collection of [DatasetEntry]s emitted by the pipeline.
 *
 * `schemaVersion` describes the shape of this file; `datasetVersion` is supplied by the operator
 * and pins one build of the content. Consumers should key on the pair.
 */
@Serializable
data class DatasetFile(
    val schemaVersion: Int,
    val datasetVersion: String,
    val generatedAt: String,
    val levels: List<String>,
    val entries: List<DatasetEntry>,
)

/** A single vocabulary Entry: Level-tagged wordlist enriched from Kaikki + optional sources. */
@Serializable
data class DatasetEntry(
    val id: String,
    val lemma: String,
    val normalizedLemma: String,
    val level: String,
    val pos: String? = null,
    val article: String? = null,
    val gender: String? = null,
    val plural: String? = null,
    val verb: VerbForms? = null,
    val translations: List<String> = emptyList(),
    val ipa: List<String> = emptyList(),
    val examples: List<Example> = emptyList(),
    /**
     * Semantic neighbours from OdeNet (the Open German WordNet). Optional — empty when the OdeNet
     * source was not supplied. Each entry names the related word and the relation kind.
     */
    val related: List<Related> = emptyList(),
    /**
     * Case-governance evidence from Universal Dependencies German GSD. Optional — empty when the UD
     * source was not supplied or the lemma had no treebank evidence. At most one per grammatical case.
     */
    val caseUsage: List<CaseUsage> = emptyList(),
    val topic: String? = null,
    val sources: EntrySources = EntrySources(),
)

/** Principal parts of a German verb (Kaikki/Wiktextract). */
@Serializable
data class VerbForms(
    val infinitive: String? = null,
    val praeteritum: String? = null,
    val partizipII: String? = null,
    /** Partizip I / present participle (`laufend`); present + participle tags in Kaikki. */
    val partizipI: String? = null,
    /** 2nd person singular present (`du läufst`). */
    val presentDu: String? = null,
    /** 3rd person singular present (`er läuft`). */
    val presentEr: String? = null,
    /** 2nd person plural present (`ihr lauft`). */
    val presentIhr: String? = null,
    val separable: Boolean? = null,
)

/**
 * A bilingual example sentence. German is authoritative; English is optional.
 *
 * `source` records provenance so consumers can attribute correctly (`kaikki`, `tatoeba`, …). It is
 * `null` for entries built before this field existed.
 */
@Serializable
data class Example(
    val de: String,
    val en: String? = null,
    val source: String? = null,
)

/**
 * One semantic relation to another word, from OdeNet.
 *
 * `relation` is one of `similar` (co-hyponym/synonym), `broader` (hypernym) or `narrower`
 * (hyponym).
 */
@Serializable
data class Related(
    val word: String,
    val relation: String,
)

/**
 * One piece of grammatical-case evidence for a noun, from a UD German GSD sentence.
 *
 * `case` is `accusative` or `dative`, `sentence` is the treebank sentence text and `gloss` is a short
 * generated hint (e.g. `Direct object`, `After "mit"`).
 */
@Serializable
data class CaseUsage(
    val case: String,
    val sentence: String,
    val gloss: String,
)

/** Provenance flags: which sources contributed to this Entry. */
@Serializable
data class EntrySources(
    val wordlist: Boolean = true,
    val kaikki: Boolean = false,
    val tatoeba: Boolean = false,
    val odenet: Boolean = false,
    val ud: Boolean = false,
)
