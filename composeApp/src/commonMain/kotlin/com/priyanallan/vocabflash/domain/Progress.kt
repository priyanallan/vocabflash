package com.priyanallan.vocabflash.domain

/** At-a-glance rating of how hard a Deck currently is for the learner (D38). */
enum class Difficulty(val label: String) {
    New("New"),
    Easy("Easy"),
    Medium("Medium"),
    Hard("Hard"),
}

/** A learner's persisted progress for a single Card, as needed to score a Deck. */
data class CardProgress(
    val entryId: String,
    val mode: StudyMode,
    val state: CardState,
    val stability: Double,
    val flagged: Boolean,
    val due: Long = 0L,
    val reps: Int = 0,
)

/** Aggregate progress for one Deck. */
data class DeckProgress(
    val totalCards: Int,
    val seen: Int,
    val mastered: Int,
    val problems: Int,
)

/** A Card counts as "mastered" once its stability reaches this many days. */
const val MASTERED_STABILITY_DAYS: Double = 21.0

/**
 * Aggregate the Cards of one level into [DeckProgress].
 * Considers every Card the deck would generate across [modes].
 */
fun computeDeckProgress(
    entries: List<Entry>,
    cards: List<CardProgress>,
    level: Level,
    modes: List<StudyMode> = ActiveStudyModes,
): DeckProgress = deckProgress(entries.filter { it.level == level }, cards, modes)

/**
 * Aggregate [DeckProgress] for an already-selected set of Entries — used by the single Deck (D54),
 * which spans every level up to the chosen maximum.
 */
fun deckProgress(
    deckEntries: List<Entry>,
    cards: List<CardProgress>,
    modes: List<StudyMode> = ActiveStudyModes,
): DeckProgress {
    val expected = studyCards(deckEntries, modes)
    val keys = expected.mapTo(mutableSetOf()) { it.entry.id to it.mode }
    val relevant = cards.filter { (it.entryId to it.mode) in keys }
    return DeckProgress(
        totalCards = expected.size,
        seen = relevant.size,
        mastered = relevant.count { it.state == CardState.Review && it.stability >= MASTERED_STABILITY_DAYS },
        problems = relevant.count {
            it.flagged || it.state == CardState.Learning || it.state == CardState.Relearning
        },
    )
}

/**
 * The Deck's Difficulty, derived from performance — never authored.
 * New until something has been studied; then driven by the share of struggling Cards.
 */
fun DeckProgress.difficulty(): Difficulty = when {
    seen == 0 -> Difficulty.New
    else -> when (val struggle = problems.toDouble() / seen) {
        in 0.0..<0.15 -> Difficulty.Easy
        in 0.15..<0.40 -> Difficulty.Medium
        else -> Difficulty.Hard
    }
}
