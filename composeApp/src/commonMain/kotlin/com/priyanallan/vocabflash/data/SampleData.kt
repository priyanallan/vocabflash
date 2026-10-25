package com.priyanallan.vocabflash.data

import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.ExampleSentence
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.PartOfSpeech

/**
 * Fallback vocabulary, used only when the bundled dataset.json is missing or malformed.
 * Real Entries are built at build time from a word source (KEFR list) enriched by Kaikki —
 * see ADR-0002 / ADR-0005.
 */
val SampleEntries: List<Entry> = listOf(
    Entry(
        id = "a1-baum", lemma = "Baum", partOfSpeech = PartOfSpeech.Noun, level = Level.A1,
        article = "der", plural = "die Bäume", translations = listOf("tree"),
        ipa = "baʊ̯m", topic = "Nature",
        examples = listOf(ExampleSentence("Der Baum ist alt.", "The tree is old.", "sample")),
    ),
    Entry(
        id = "a1-haus", lemma = "Haus", partOfSpeech = PartOfSpeech.Noun, level = Level.A1,
        article = "das", plural = "die Häuser", translations = listOf("house"),
        ipa = "haʊ̯s", topic = "Home",
        examples = listOf(ExampleSentence("Das Haus ist groß.", "The house is big.", "sample")),
    ),
    Entry(
        id = "a1-familie", lemma = "Familie", partOfSpeech = PartOfSpeech.Noun, level = Level.A1,
        article = "die", plural = "die Familien", translations = listOf("family"),
        ipa = "faˈmiːli̯ə", topic = "Family",
        examples = listOf(ExampleSentence("Meine Familie ist klein.", "My family is small.", "sample")),
    ),
    Entry(
        id = "a1-gehen", lemma = "gehen", partOfSpeech = PartOfSpeech.Verb, level = Level.A1,
        principalParts = listOf("gehen", "ging", "ist gegangen"),
        translations = listOf("to go", "to walk"), ipa = "ˈɡeːən", topic = "Motion",
        examples = listOf(ExampleSentence("Ich gehe nach Hause.", "I am going home.", "sample")),
    ),
    Entry(
        id = "a2-wichtig", lemma = "wichtig", partOfSpeech = PartOfSpeech.Adjective, level = Level.A2,
        translations = listOf("important"), ipa = "ˈvɪçtɪç", topic = "General",
        examples = listOf(ExampleSentence("Das ist sehr wichtig.", "That is very important.", "sample")),
    ),
    Entry(
        id = "b1-auseinandersetzung", lemma = "Auseinandersetzung", partOfSpeech = PartOfSpeech.Noun,
        level = Level.B1, article = "die", plural = "die Auseinandersetzungen",
        translations = listOf("discussion", "argument"), ipa = "ˌaʊ̯sʔaɪ̯nˌandɐˈzɛt͡sʊŋ",
        topic = "Society",
        examples = listOf(
            ExampleSentence(
                "Es gab eine lange Auseinandersetzung.",
                "There was a long discussion.",
                "sample",
            ),
        ),
    ),
)
