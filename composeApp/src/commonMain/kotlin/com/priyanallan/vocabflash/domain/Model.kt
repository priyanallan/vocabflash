package com.priyanallan.vocabflash.domain

/** A CEFR proficiency band. */
enum class Level(val code: String) {
    A1("A1"), A2("A2"), B1("B1"), B2("B2")
}

/**
 * Part of speech, as displayed on the card back. Values map from the dataset's `pos`
 * field (Kaikki/Wiktextract tags) in [data.DatasetLoader]; unknown tags fall back to [Other].
 */
enum class PartOfSpeech(val label: String) {
    Noun("Noun"),
    Verb("Verb"),
    Adjective("Adjective"),
    Adverb("Adverb"),
    Pronoun("Pronoun"),
    Preposition("Preposition"),
    Conjunction("Conjunction"),
    Determiner("Determiner"),
    Article("Article"),
    Numeral("Numeral"),
    Interjection("Interjection"),
    Particle("Particle"),
    ProperNoun("Proper noun"),
    Contraction("Contraction"),
    Phrase("Phrase"),
    Other("Other"),
}

/** How a Card asks for retrieval. One Entry yields one Card per active mode. */
enum class StudyMode(val label: String) {
    Recognition("German → English"),
    Production("English → German"),
    GenderDrill("Article drill"),
}

/** The learner's self-assessment after a Review. Values match the FSRS grade scale. */
enum class Rating(val value: Int, val label: String) {
    Again(1, "Again"),
    Hard(2, "Hard"),
    Good(3, "Good"),
    Easy(4, "Easy");

    companion object {
        fun fromValue(v: Int): Rating = entries.first { it.value == v }
    }
}

enum class CardState { New, Learning, Review, Relearning }

/** A worked example sentence, with its source so attribution can be shown (D57). */
data class ExampleSentence(
    val de: String,
    val en: String? = null,
    val source: String? = null,
)

/** How a related word relates to the Entry (D47/D51). */
enum class RelatedRelation(val label: String) {
    Broader("Überbegriff"),
    Narrower("Unterbegriff"),
    Similar("Ähnlich"),
}

data class RelatedWord(val word: String, val relation: RelatedRelation)

/** A grammatical case, as shown in the Word Details case panels (D51/D56). */
enum class GrammaticalCase(val label: String) {
    Nominative("NOMINATIV"),
    Accusative("AKKUSATIV"),
    Dative("DATIV"),
    Genitive("GENITIV"),
}

data class CaseUsage(
    val case: GrammaticalCase,
    val sentence: String,
    val gloss: String? = null,
)

/** Verb forms beyond the principal parts — used by the details sheet (D57). */
data class Conjugation(
    val presentDu: String? = null,
    val presentEr: String? = null,
    val presentIhr: String? = null,
    val partizipI: String? = null,
)

/**
 * A single German lemma and its linguistic attributes.
 * Fields are tiered: core (always), German-specific (where applicable), enrichment (optional).
 */
data class Entry(
    val id: String,
    val lemma: String,
    val partOfSpeech: PartOfSpeech,
    val level: Level,
    val article: String? = null,          // der / die / das — nouns only
    val plural: String? = null,           // e.g. "die Bäume"
    val principalParts: List<String> = emptyList(), // Infinitiv, Präteritum, Perfekt
    val separable: Boolean = false,
    val translations: List<String> = emptyList(),
    val ipa: String? = null,
    val examples: List<ExampleSentence> = emptyList(),
    val related: List<RelatedWord> = emptyList(),
    val caseUsages: List<CaseUsage> = emptyList(),
    val conjugation: Conjugation? = null,
    val topic: String? = null,
) {
    /** Form shown on the front of a recognition card, e.g. "der Baum". */
    val displayForm: String get() = if (article != null) "$article $lemma" else lemma

    /** The one example shown on the back of a card; the rest live in Word Details. */
    val primaryExample: ExampleSentence? get() = examples.firstOrNull()
}

/**
 * Longest example sentence shown for a level — roughly "two lines" on a phone.
 * Shorter sentences are also simpler, which matters most at A1.
 */
fun exampleCharLimit(level: Level): Int = when (level) {
    Level.A1 -> 70
    Level.A2 -> 85
    Level.B1 -> 110
    Level.B2 -> 130
}

/** Examples that are short enough to read at this Entry's level, shortest first, at most [limit]. */
fun Entry.readableExamples(limit: Int = 10): List<ExampleSentence> {
    val cap = exampleCharLimit(level)
    return examples.filter { it.de.length <= cap }.sortedBy { it.de.length }.take(limit)
}

/**
 * The per-Card memory state that decides when the Card is next due.
 * Retrievability is computed on demand, never stored.
 */
data class ScheduleState(
    val state: CardState = CardState.New,
    val due: Long = 0L,
    val lastReview: Long? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val scheduledDays: Int = 0,
    val elapsedDays: Int = 0,
)

/** A single study prompt generated from one Entry for one StudyMode. */
data class Card(
    val entry: Entry,
    val mode: StudyMode,
    val schedule: ScheduleState = ScheduleState(),
)

/** A level-based study collection of Cards. */
class Deck(val level: Level, val cards: List<Card>) {
    val id: String get() = level.code

    companion object {
        fun fromEntries(level: Level, entries: List<Entry>, modes: List<StudyMode>): Deck =
            Deck(
                level = level,
                cards = entries.flatMap { entry -> modes.map { mode -> Card(entry, mode) } },
            )
    }
}

/** Levels currently shipped — B2 is not in the bundled open dataset yet (D10). */
val SupportedLevels: List<Level> = listOf(Level.A1, Level.A2, Level.B1)

/** Study modes active in v1 (D8). Gender drill only applies to nouns. */
val ActiveStudyModes: List<StudyMode> =
    listOf(StudyMode.Recognition, StudyMode.Production, StudyMode.GenderDrill)

/** One Card to study: an Entry asked in one StudyMode. */
data class StudyCard(val entry: Entry, val mode: StudyMode)

/** Does this StudyMode apply to this Entry? */
fun StudyMode.appliesTo(entry: Entry): Boolean = when (this) {
    StudyMode.GenderDrill -> entry.article != null
    else -> true
}

/** Generate the Cards for a set of Entries across the given StudyModes. */
fun studyCards(entries: List<Entry>, modes: List<StudyMode> = ActiveStudyModes): List<StudyCard> =
    entries.flatMap { entry ->
        modes.filter { it.appliesTo(entry) }.map { mode -> StudyCard(entry, mode) }
    }
