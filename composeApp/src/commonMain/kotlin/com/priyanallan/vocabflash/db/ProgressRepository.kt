package com.priyanallan.vocabflash.db

import app.cash.sqldelight.db.SqlDriver
import com.priyanallan.vocabflash.domain.CardProgress
import com.priyanallan.vocabflash.domain.CardState
import com.priyanallan.vocabflash.domain.DEFAULT_NEW_PER_DAY
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.SETTING_ENABLED_LEVELS
import com.priyanallan.vocabflash.domain.SETTING_MAX_LEVEL
import com.priyanallan.vocabflash.domain.SETTING_NEW_PER_DAY
import com.priyanallan.vocabflash.domain.ScheduleState
import com.priyanallan.vocabflash.domain.StudyMode
import com.priyanallan.vocabflash.domain.SupportedLevels

/**
 * Persistence for per-Card Schedule State, Review Deck membership (D36) and progress counters.
 * Sits behind this class so a remote/synced implementation could replace it later (ADR-0003).
 */
class ProgressRepository(driver: SqlDriver) {

    private val queries = VocabDatabase(driver).cardStateQueries

    fun load(entryId: String, mode: String): ScheduleState? =
        queries.selectByEntryMode(entryId, mode).executeAsOneOrNull()?.let { row ->
            ScheduleState(
                state = runCatching { CardState.valueOf(row.state) }.getOrDefault(CardState.New),
                due = row.due,
                lastReview = row.last_review,
                stability = row.stability,
                difficulty = row.difficulty,
                reps = row.reps.toInt(),
                lapses = row.lapses.toInt(),
                scheduledDays = row.scheduled_days.toInt(),
                elapsedDays = row.elapsed_days.toInt(),
            )
        }

    /** Persists Schedule State, preserving the Review Deck flag. */
    fun save(entryId: String, mode: String, state: ScheduleState) {
        queries.upsert(
            entry_id = entryId,
            mode = mode,
            state = state.state.name,
            due = state.due,
            last_review = state.lastReview,
            stability = state.stability,
            difficulty = state.difficulty,
            reps = state.reps.toLong(),
            lapses = state.lapses.toLong(),
            scheduled_days = state.scheduledDays.toLong(),
            elapsed_days = state.elapsedDays.toLong(),
            flagged = if (isFlagged(entryId, mode)) 1L else 0L,
        )
    }

    fun isFlagged(entryId: String, mode: String): Boolean =
        queries.isFlagged(entryId, mode).executeAsOneOrNull() == 1L

    /** Adds/removes a Card from the Review Deck, creating the row if needed. */
    fun setFlagged(entryId: String, mode: String, flagged: Boolean) {
        queries.ensureRow(entryId, mode)
        if (flagged) queries.flagCard(entryId, mode) else queries.unflagCard(entryId, mode)
    }

    /** Entry ids currently in the Review Deck (D36). */
    fun flaggedEntryIds(): List<String> =
        queries.selectFlagged().executeAsList().map { it.entry_id }

    /** Cards currently in the Review Deck, with their StudyMode. */
    fun flaggedCards(): List<CardProgress> = queries.selectFlagged().executeAsList().map { row ->
        CardProgress(
            entryId = row.entry_id,
            mode = runCatching { StudyMode.valueOf(row.mode) }.getOrDefault(StudyMode.Recognition),
            state = runCatching { CardState.valueOf(row.state) }.getOrDefault(CardState.New),
            stability = row.stability,
            flagged = true,
            due = row.due,
            reps = row.reps.toInt(),
        )
    }

    fun seenCount(): Long = queries.countAll().executeAsOne()

    fun flaggedCount(): Long = queries.countFlagged().executeAsOne()

    /** All persisted Cards, for computing Deck Difficulty (D38). */
    fun allCards(): List<CardProgress> = queries.selectAll().executeAsList().map { row ->
        CardProgress(
            entryId = row.entry_id,
            mode = runCatching { StudyMode.valueOf(row.mode) }.getOrDefault(StudyMode.Recognition),
            state = runCatching { CardState.valueOf(row.state) }.getOrDefault(CardState.New),
            stability = row.stability,
            flagged = row.flagged == 1L,
            due = row.due,
            reps = row.reps.toInt(),
        )
    }

    // ── Daily new-Card allowance (D14) ──────────────────────────────────────────

    /** New Cards introduced on [day] (`YYYY-MM-DD`, local midnight boundary). */
    fun newIntroduced(day: String): Int = queries.selectDailyNew(day).executeAsOneOrNull()?.toInt() ?: 0

    fun setNewIntroduced(day: String, count: Int) {
        queries.upsertDailyNew(day, count.toLong())
    }

    // ── Settings ───────────────────────────────────────────────────────────────

    fun getSetting(key: String, default: String): String =
        queries.getSetting(key).executeAsOneOrNull() ?: default

    fun setSetting(key: String, value: String) {
        queries.putSetting(key, value)
    }

    fun newPerDay(): Int =
        getSetting(SETTING_NEW_PER_DAY, DEFAULT_NEW_PER_DAY.toString()).toIntOrNull()
            ?: DEFAULT_NEW_PER_DAY

    fun setNewPerDay(count: Int) = setSetting(SETTING_NEW_PER_DAY, count.toString())

    /** Levels the learner has enabled (D39). Defaults to every supported level. */
    fun enabledLevels(): Set<Level> {
        val stored = getSetting(SETTING_ENABLED_LEVELS, SupportedLevels.joinToString(",") { it.code })
        return stored.split(",")
            .mapNotNull { code -> Level.entries.firstOrNull { it.code.equals(code.trim(), true) } }
            .toSet()
    }

    fun setEnabledLevels(levels: Set<Level>) =
        setSetting(SETTING_ENABLED_LEVELS, levels.joinToString(",") { it.code })

    /**
     * The highest CEFR level the learner studies (D45). Everything at or below it is available.
     * Migrates once from the older `enabled_levels` key if present.
     */
    fun maxLevel(): Level {
        val stored = getSetting(SETTING_MAX_LEVEL, "")
        Level.entries.firstOrNull { it.code.equals(stored, true) }?.let { return it }

        val legacy = getSetting(SETTING_ENABLED_LEVELS, "")
        if (legacy.isNotBlank()) {
            val highest = legacy.split(",")
                .mapNotNull { code -> Level.entries.firstOrNull { it.code.equals(code.trim(), true) } }
                .maxByOrNull { it.ordinal }
            if (highest != null) return highest
        }
        return SupportedLevels.last()
    }

    fun setMaxLevel(level: Level) = setSetting(SETTING_MAX_LEVEL, level.code)
}
