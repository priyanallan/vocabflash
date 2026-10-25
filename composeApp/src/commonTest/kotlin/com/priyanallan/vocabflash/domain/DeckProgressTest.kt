package com.priyanallan.vocabflash.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Deck Difficulty (D38) is derived from performance — these lock the derivation rules. */
class DeckProgressTest {

    // No articles -> these Entries get Recognition + Production Cards, but no gender drill (D8).
    private val entries = listOf(
        Entry("a1-haus-noun", "Haus", PartOfSpeech.Noun, Level.A1),
        Entry("a1-baum-noun", "Baum", PartOfSpeech.Noun, Level.A1),
        Entry("a2-wichtig-adj", "wichtig", PartOfSpeech.Adjective, Level.A2),
    )

    private fun card(
        id: String,
        state: CardState = CardState.Review,
        stability: Double = 30.0,
        flagged: Boolean = false,
        mode: StudyMode = StudyMode.Recognition,
    ) = CardProgress(id, mode, state, stability, flagged)

    @Test
    fun unseenDeckCountsEveryMode() {
        val progress = computeDeckProgress(entries, emptyList(), Level.A1)
        assertEquals(4, progress.totalCards) // 2 Entries × (Recognition + Production)
        assertEquals(0, progress.seen)
        assertEquals(Difficulty.New, progress.difficulty())
    }

    @Test
    fun otherLevelsAreExcluded() {
        val cards = listOf(card("a2-wichtig-adj"))
        assertEquals(0, computeDeckProgress(entries, cards, Level.A1).seen)
    }

    @Test
    fun onlyRequestedModesAreCounted() {
        val cards = listOf(card("a1-haus-noun", mode = StudyMode.Production))
        val progress = computeDeckProgress(entries, cards, Level.A1, modes = listOf(StudyMode.Recognition))
        assertEquals(2, progress.totalCards)
        assertEquals(0, progress.seen)
    }

    @Test
    fun genderDrillIsOnlyGeneratedForNouns() {
        val withArticle = listOf(
            Entry("a1-haus-noun", "Haus", PartOfSpeech.Noun, Level.A1, article = "das"),
            Entry("a2-wichtig-adj", "wichtig", PartOfSpeech.Adjective, Level.A2),
        )
        val cards = studyCards(withArticle)
        assertEquals(5, cards.size) // Haus: 3 modes, wichtig: 2 modes
        assertTrue(cards.any { it.entry.id == "a1-haus-noun" && it.mode == StudyMode.GenderDrill })
        assertTrue(cards.none { it.entry.id == "a2-wichtig-adj" && it.mode == StudyMode.GenderDrill })
    }

    @Test
    fun masteredCardsMakeDeckEasy() {
        val cards = listOf(
            card("a1-haus-noun", stability = 30.0),
            card("a1-baum-noun", stability = 40.0),
        )
        val progress = computeDeckProgress(entries, cards, Level.A1)
        assertEquals(2, progress.mastered)
        assertEquals(0, progress.problems)
        assertEquals(Difficulty.Easy, progress.difficulty())
    }

    @Test
    fun lowStabilityReviewCardIsNotMastered() {
        val cards = listOf(card("a1-haus-noun", stability = MASTERED_STABILITY_DAYS - 1))
        assertEquals(0, computeDeckProgress(entries, cards, Level.A1).mastered)
    }

    @Test
    fun flaggedOrLearningCardsRaiseDifficulty() {
        val cards = listOf(
            card("a1-haus-noun", state = CardState.Relearning, stability = 1.0, flagged = true),
            card("a1-baum-noun"),
        )
        val progress = computeDeckProgress(entries, cards, Level.A1)
        assertEquals(1, progress.problems)
        assertEquals(Difficulty.Hard, progress.difficulty()) // 1/2 = 0.5 ≥ 0.40
    }

    @Test
    fun partialStruggleIsMedium() {
        val three = listOf(
            Entry("a1-1", "e1", PartOfSpeech.Noun, Level.A1),
            Entry("a1-2", "e2", PartOfSpeech.Noun, Level.A1),
            Entry("a1-3", "e3", PartOfSpeech.Noun, Level.A1),
        )
        val cards = listOf(
            card("a1-1"),
            card("a1-2"),
            card("a1-3", state = CardState.Learning, stability = 0.5),
        )
        assertEquals(Difficulty.Medium, computeDeckProgress(three, cards, Level.A1).difficulty()) // 1/3
    }
}
