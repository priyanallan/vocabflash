package com.priyanallan.vocabflash.data

import com.priyanallan.vocabflash.domain.CaseUsage
import com.priyanallan.vocabflash.domain.Conjugation
import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.ExampleSentence
import com.priyanallan.vocabflash.domain.GrammaticalCase
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.PartOfSpeech
import com.priyanallan.vocabflash.domain.RelatedRelation
import com.priyanallan.vocabflash.domain.RelatedWord
import com.priyanallan.vocabflash.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val DATASET_PATH = "files/dataset.json"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Loads the bundled Dataset produced by the wordlist → Kaikki pipeline (ADR-0005).
 * Falls back to [SampleEntries] if the resource is missing or malformed, so the app never
 * fails to start on a bad dataset.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadBundledDataset(): List<Entry> = runCatching {
    val text = Res.readBytes(DATASET_PATH).decodeToString()
    json.decodeFromString<DatasetFileDto>(text).entries.mapNotNull { it.toDomain() }
}.getOrElse { SampleEntries }

internal fun DatasetEntryDto.toDomain(): Entry? {
    val levelValue = Level.entries.firstOrNull { it.code.equals(level, ignoreCase = true) }
        ?: return null
    val partOfSpeech = when (pos?.lowercase()) {
        "noun" -> PartOfSpeech.Noun
        "verb" -> PartOfSpeech.Verb
        "adj", "adjective" -> PartOfSpeech.Adjective
        "adv", "adverb" -> PartOfSpeech.Adverb
        "phrase" -> PartOfSpeech.Phrase
        else -> PartOfSpeech.Other
    }
    val principalParts = verb?.let { v ->
        listOfNotNull(v.infinitive, v.praeteritum, v.partizipII)
    }.orEmpty()

    return Entry(
        id = id,
        lemma = lemma,
        partOfSpeech = partOfSpeech,
        level = levelValue,
        article = article,
        plural = plural,
        principalParts = principalParts,
        translations = translations,
        ipa = ipa.firstOrNull()?.trim('/'),
        examples = examples.map { ExampleSentence(de = it.de, en = it.en, source = it.source) },
        related = related.mapNotNull { dto ->
            relationOf(dto.relation)?.let { RelatedWord(word = dto.word, relation = it) }
        },
        caseUsages = caseUsage.mapNotNull { dto ->
            caseOf(dto.case)?.let { CaseUsage(case = it, sentence = dto.sentence, gloss = dto.gloss) }
        },
        conjugation = verb?.let {
            Conjugation(
                presentDu = it.presentDu,
                presentEr = it.presentEr,
                presentIhr = it.presentIhr,
                partizipI = it.partizipI,
            )
        }?.takeIf { it != Conjugation() },
        topic = topic,
    )
}

private fun relationOf(raw: String?): RelatedRelation? = when (raw?.lowercase()) {
    "broader", "hypernym" -> RelatedRelation.Broader
    "narrower", "hyponym" -> RelatedRelation.Narrower
    "similar", "cohyponym", "synonym" -> RelatedRelation.Similar
    else -> null
}

private fun caseOf(raw: String?): GrammaticalCase? = when (raw?.lowercase()) {
    "nominative", "nom" -> GrammaticalCase.Nominative
    "accusative", "acc" -> GrammaticalCase.Accusative
    "dative", "dat" -> GrammaticalCase.Dative
    "genitive", "gen" -> GrammaticalCase.Genitive
    else -> null
}
