@file:OptIn(ExperimentalTime::class)

package com.priyanallan.vocabflash.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Default number of new Cards introduced per day (D14). Learner-adjustable in Settings. */
const val DEFAULT_NEW_PER_DAY: Int = 20

/** Setting key for the daily new-Card allowance. */
const val SETTING_NEW_PER_DAY: String = "new_per_day"

/** Setting key for the enabled CEFR levels (comma-separated codes). */
const val SETTING_ENABLED_LEVELS: String = "enabled_levels"

/** Setting key for the highest CEFR level the learner studies (D45). */
const val SETTING_MAX_LEVEL: String = "max_level"

/**
 * A calendar day key (`YYYY-MM-DD`) in the given time zone.
 * The day boundary is **local midnight** — the daily new-Card allowance resets then (D14).
 */
fun localDayKey(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date.toString()

/** The minimum needed to decide whether a Card is due or new. */
data class QueueInfo(val reps: Int, val due: Long) {
    companion object {
        val New: QueueInfo = QueueInfo(reps = 0, due = 0L)
    }
}

/** The Cards to study right now: everything due, plus up to the day's remaining new Cards. */
data class StudyQueue(
    val due: List<StudyCard>,
    val new: List<StudyCard>,
) {
    val all: List<StudyCard> get() = due + new
    val isEmpty: Boolean get() = all.isEmpty()
}

/**
 * Build the study queue (D14).
 *
 * A Card is *new* until it has been reviewed once ([QueueInfo.reps] == 0); a previously reviewed Card
 * is *due* when its `due` time has passed. New Cards are capped to whatever remains of
 * [newPerDay] after [newIntroducedToday].
 */
fun buildStudyQueue(
    cards: List<StudyCard>,
    infoOf: (StudyCard) -> QueueInfo,
    now: Long,
    newPerDay: Int = DEFAULT_NEW_PER_DAY,
    newIntroducedToday: Int = 0,
): StudyQueue {
    val due = ArrayList<StudyCard>()
    val fresh = ArrayList<StudyCard>()
    for (card in cards) {
        val info = infoOf(card)
        if (info.reps == 0) fresh += card else if (info.due <= now) due += card
    }
    val remaining = (newPerDay - newIntroducedToday).coerceAtLeast(0)
    return StudyQueue(due = due, new = fresh.take(remaining))
}

/**
 * Reorder so that two Cards of the same Entry are never adjacent (D49): each Card's Entry must
 * differ from the previous [minGap] Cards' Entries. Deterministic; falls back to relaxing the rule
 * only when no other Entry remains.
 */
fun spaceOut(cards: List<StudyCard>, minGap: Int = 3): List<StudyCard> {
    if (minGap <= 0 || cards.size <= 1) return cards
    val remaining = cards.toMutableList()
    val out = ArrayList<StudyCard>(cards.size)
    while (remaining.isNotEmpty()) {
        val recent = out.takeLast(minGap).mapTo(mutableSetOf()) { it.entry.id }
        val pick = remaining.firstOrNull { it.entry.id !in recent } ?: remaining.first()
        remaining.remove(pick)
        out += pick
    }
    return out
}
