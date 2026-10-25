package com.priyanallan.vocabflash.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Reference-vector checks for the hand-rolled FSRS-6 scheduler (see ADR-0001). */
class FsrsTest {

    @Test
    fun retrievabilityAtStabilityIs90Percent() {
        val s = 10.0
        assertEquals(0.9, Fsrs.forgettingCurve(s, s), 0.001)
    }

    @Test
    fun intervalAtRequestedRetentionEqualsStability() {
        val s = 10.0
        assertEquals(10, Fsrs.nextInterval(s))
    }

    @Test
    fun newCardStabilityMatchesDefaultWeight() {
        val state = Fsrs.nextState(null, elapsedDays = 0, rating = Rating.Good)
        assertEquals(Fsrs.DEFAULT_W[2], state.stability, 1e-9)
    }

    @Test
    fun newCardDifficultyMatchesFormula() {
        val state = Fsrs.nextState(null, elapsedDays = 0, rating = Rating.Good)
        assertEquals(Fsrs.initDifficulty(Rating.Good), state.difficulty, 1e-9)
    }

    @Test
    fun ratingAgainIncreasesLapses() {
        val first = Fsrs.nextState(null, 0, Rating.Good)
        val after = Fsrs.nextState(first, elapsedDays = first.scheduledDays, rating = Rating.Again)
        assertEquals(1, after.lapses)
    }

    @Test
    fun previewIntervalsAreOrdered() {
        val preview = Fsrs.previewIntervals(null, 0)
        // For a new card, Easy should schedule at least as far out as Good.
        assert(preview.getValue(Rating.Easy) >= preview.getValue(Rating.Good))
    }
}
