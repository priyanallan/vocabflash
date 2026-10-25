package com.priyanallan.vocabflash.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.ScheduleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Exercises the Review Deck (D36) persistence against a real in-memory SQLite database. */
class ProgressRepositoryTest {

    private fun repository(): ProgressRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VocabDatabase.Schema.create(driver)
        return ProgressRepository(driver)
    }

    @Test
    fun flaggingAddsAndRemovesFromReviewDeck() {
        val repo = repository()
        assertFalse(repo.isFlagged("a1-haus-noun", "Recognition"))

        repo.setFlagged("a1-haus-noun", "Recognition", true)
        assertTrue(repo.isFlagged("a1-haus-noun", "Recognition"))
        assertEquals(listOf("a1-haus-noun"), repo.flaggedEntryIds())
        assertEquals(1L, repo.flaggedCount())

        repo.setFlagged("a1-haus-noun", "Recognition", false)
        assertFalse(repo.isFlagged("a1-haus-noun", "Recognition"))
        assertEquals(0, repo.flaggedEntryIds().size)
    }

    @Test
    fun savingScheduleStateDoesNotClearTheFlag() {
        val repo = repository()
        repo.setFlagged("b1-geborenheit-noun", "Recognition", true)

        repo.save("b1-geborenheit-noun", "Recognition", ScheduleState(reps = 1, scheduledDays = 3))

        assertTrue(
            repo.isFlagged("b1-geborenheit-noun", "Recognition"),
            "save() must preserve Review Deck membership",
        )
        assertEquals(1L, repo.seenCount())
    }

    @Test
    fun scheduleStateRoundTrips() {
        val repo = repository()
        repo.save("a1-haus-noun", "Recognition", ScheduleState(reps = 2, lapses = 1, scheduledDays = 5))

        val loaded = repo.load("a1-haus-noun", "Recognition")!!
        assertEquals(2, loaded.reps)
        assertEquals(1, loaded.lapses)
        assertEquals(5, loaded.scheduledDays)
    }

    @Test
    fun enabledLevelsDefaultToAllSupportedAndPersist() {
        val repo = repository()
        assertEquals(setOf(Level.A1, Level.A2, Level.B1), repo.enabledLevels())

        repo.setEnabledLevels(setOf(Level.A2))
        assertEquals(setOf(Level.A2), repo.enabledLevels())

        repo.setEnabledLevels(emptySet())
        assertEquals(emptySet(), repo.enabledLevels())
    }

    @Test
    fun newPerDayDefaultsToTwentyAndPersists() {
        val repo = repository()
        assertEquals(20, repo.newPerDay())

        repo.setNewPerDay(30)
        assertEquals(30, repo.newPerDay())
    }

    @Test
    fun maxLevelDefaultsToTheHighestSupportedAndPersists() {
        val repo = repository()
        assertEquals(Level.B1, repo.maxLevel())

        repo.setMaxLevel(Level.A2)
        assertEquals(Level.A2, repo.maxLevel())
    }

    @Test
    fun maxLevelMigratesFromLegacyEnabledLevels() {
        val repo = repository()
        repo.setSetting("enabled_levels", "A1,A2")

        assertEquals(Level.A2, repo.maxLevel())
    }
}
