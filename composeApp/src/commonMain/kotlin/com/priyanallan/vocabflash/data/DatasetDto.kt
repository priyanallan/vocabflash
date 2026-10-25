package com.priyanallan.vocabflash.data

import kotlinx.serialization.Serializable

/**
 * Wire format produced by `tools/dataset-builder`. Kept separate from the domain model so the
 * bundled schema can evolve without leaking into UI/business code. All enrichment fields are
 * optional — the app degrades gracefully when a field is absent (D57).
 */
@Serializable
data class DatasetFileDto(
    val schemaVersion: Int = 1,
    val datasetVersion: String = "dev",
    val generatedAt: String? = null,
    val levels: List<String> = emptyList(),
    val entries: List<DatasetEntryDto> = emptyList(),
)

@Serializable
data class DatasetEntryDto(
    val id: String,
    val lemma: String,
    val normalizedLemma: String? = null,
    val level: String,
    val pos: String? = null,
    val article: String? = null,
    val gender: String? = null,
    val plural: String? = null,
    val verb: VerbFormsDto? = null,
    val translations: List<String> = emptyList(),
    val ipa: List<String> = emptyList(),
    val examples: List<ExampleDto> = emptyList(),
    val related: List<RelatedWordDto> = emptyList(),
    val caseUsage: List<CaseUsageDto> = emptyList(),
    val topic: String? = null,
    val sources: SourcesDto? = null,
)

@Serializable
data class VerbFormsDto(
    val infinitive: String? = null,
    val praeteritum: String? = null,
    val partizipII: String? = null,
    val presentDu: String? = null,
    val presentEr: String? = null,
    val presentIhr: String? = null,
    val partizipI: String? = null,
)

@Serializable
data class ExampleDto(
    val de: String,
    val en: String? = null,
    val source: String? = null,
)

@Serializable
data class RelatedWordDto(
    val word: String,
    val relation: String? = null,
)

@Serializable
data class CaseUsageDto(
    val case: String? = null,
    val sentence: String,
    val gloss: String? = null,
)

@Serializable
data class SourcesDto(val kaikki: Boolean = false)
