@file:OptIn(ExperimentalTime::class)

package com.priyanallan.vocabflash.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/** The daily study queue (D14) and its local-midnight day boundary. */
class QueueTest {

    private fun card(id: String, mode: StudyMode = StudyMode.Recognition) =
        StudyCard(Entry(id, id, PartOfSpeech.Noun, Level.A1), mode)

    @Test
    fun dueReviewsAreAlwaysIncludedEvenWhenNewAllowanceIsZero() {
        val cards = listOf(card("a"), card("b"))
        val info = mapOf(
            cards[0] to QueueInfo(reps = 2, due = 100),
            cards[1] to QueueInfo(reps = 3, due = 10_000),
        )
        val queue = buildStudyQueue(cards, { info.getValue(it) }, now = 500, newPerDay = 0, newIntroducedToday = 0)
        assertEquals(listOf(cards[0]), queue.due)
        assertEquals(emptyList(), queue.new)
    }

    @Test
    fun futureCardsAreNotDue() {
        val card = card("a")
        val queue = buildStudyQueue(listOf(card), { QueueInfo(reps = 1, due = 10_000) }, now = 5_000)
        assertTrue(queue.due.isEmpty())
    }

    @Test
    fun newCardsAreCappedByTheRemainingDailyAllowance() {
        val cards = (1..5).map { card("c$it") }
        val queue = buildStudyQueue(cards, { QueueInfo.New }, now = 0, newPerDay = 3, newIntroducedToday = 1)
        assertEquals(2, queue.new.size)
    }

    @Test
    fun noNewCardsOnceTheAllowanceIsUsedUp() {
        val cards = (1..3).map { card("c$it") }
        val queue = buildStudyQueue(cards, { QueueInfo.New }, now = 0, newPerDay = 20, newIntroducedToday = 20)
        assertTrue(queue.isEmpty)
    }

    @Test
    fun dueCardsComeBeforeNewCards() {
        val reviewed = card("reviewed")
        val fresh = card("fresh")
        val queue = buildStudyQueue(
            cards = listOf(fresh, reviewed),
            infoOf = { if (it == reviewed) QueueInfo(reps = 1, due = 0) else QueueInfo.New },
            now = 1_000,
        )
        assertEquals(listOf(reviewed, fresh), queue.all)
    }

    @Test
    fun defaultAllowanceIsTwenty() {
        assertEquals(20, DEFAULT_NEW_PER_DAY)
    }

    @Test
    fun dayKeyIsIsoDateAndRollsAtLocalMidnight() {
        val midnight = LocalDate(2026, 10, 7).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        assertEquals("2026-10-07", localDayKey(midnight, TimeZone.UTC))
        assertEquals("2026-10-06", localDayKey(midnight - 1, TimeZone.UTC))
        assertEquals("2026-10-07", localDayKey(midnight + 3_600_000, TimeZone.UTC))
    }

    @Test
    fun spacingKeepsAnEntrysModesApart() {
        // 4 entries x 3 modes: a gap of 3 is satisfiable (3 entries could never satisfy it).
        val entries = listOf(
            Entry("a1-haus", "Haus", PartOfSpeech.Noun, Level.A1, article = "das"),
            Entry("a1-baum", "Baum", PartOfSpeech.Noun, Level.A1, article = "der"),
            Entry("a1-tisch", "Tisch", PartOfSpeech.Noun, Level.A1, article = "der"),
            Entry("a1-stuhl", "Stuhl", PartOfSpeech.Noun, Level.A1, article = "der"),
        )
        // Entry-by-entry order: e1 x3, e2 x3, e3 x3, e4 x3.
        val ordered = entries.flatMap { entry -> studyCards(listOf(entry)) }

        val spaced = spaceOut(ordered, minGap = 3)

        assertEquals(12, spaced.size)
        spaced.forEachIndexed { i, card ->
            val gap = spaced.subList(maxOf(0, i - 3), i)
            assertTrue(
                gap.none { it.entry.id == card.entry.id },
                "entry ${card.entry.id} repeated within the gap at $i",
            )
        }
    }

    @Test
    fun spacingFallsBackWhenItCannotSatisfyTheGap() {
        val only = Entry("a1-haus", "Haus", PartOfSpeech.Noun, Level.A1, article = "das")
        val cards = studyCards(listOf(only))
        assertEquals(cards.size, spaceOut(cards).size)
    }
}
