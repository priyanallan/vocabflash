package com.priyanallan.vocabflash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.priyanallan.vocabflash.db.ProgressRepository
import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.DEFAULT_NEW_PER_DAY
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.QueueInfo
import com.priyanallan.vocabflash.domain.StudyCard
import com.priyanallan.vocabflash.domain.SupportedLevels
import com.priyanallan.vocabflash.domain.buildStudyQueue
import com.priyanallan.vocabflash.domain.computeDeckProgress
import com.priyanallan.vocabflash.domain.deckProgress
import com.priyanallan.vocabflash.domain.difficulty
import com.priyanallan.vocabflash.domain.localDayKey
import com.priyanallan.vocabflash.domain.studyCards
import com.priyanallan.vocabflash.theme.KlarrGradient
import com.priyanallan.vocabflash.theme.difficultyColor
import kotlin.math.roundToInt

/** Home screen: one Deck per level, each with a Difficulty indicator at the bottom (D33/D38). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DeckListScreen(
    entries: List<Entry>,
    maxLevel: Level,
    repository: ProgressRepository?,
    grammarCount: Int,
    onStart: () -> Unit,
    onOpenGrammar: () -> Unit,
) {    val cards = remember(repository, entries) { repository?.allCards().orEmpty() }
    val now = remember { Clock.System.now().toEpochMilliseconds() }
    val day = remember(now) { localDayKey(now) }
    val newPerDay = remember(repository) { repository?.newPerDay() ?: DEFAULT_NEW_PER_DAY }
    val introducedToday = remember(repository, day) { repository?.newIntroduced(day) ?: 0 }

    val deckCards = remember(entries) { studyCards(entries) }
    val queue = remember(deckCards, cards, now, newPerDay, introducedToday) {
        buildStudyQueue(
            cards = deckCards,
            infoOf = { card ->
                cards.firstOrNull { it.entryId == card.entry.id && it.mode == card.mode }
                    ?.let { QueueInfo(it.reps, it.due) } ?: QueueInfo.New
            },
            now = now,
            newPerDay = newPerDay,
            newIntroducedToday = introducedToday,
        )
    }
    val progress = remember(entries, cards) { deckProgress(entries, cards) }
    val difficulty = progress.difficulty()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${queue.all.size} cards ready · new cards capped at $newPerDay/day",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(KlarrGradient),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                maxLevel.code,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "${queue.due.size} due · ${queue.new.size} new",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${entries.size} words · ${progress.seen} seen · ${progress.mastered} mastered",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.alpha(0.65f),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(if (queue.all.isEmpty()) "Nothing due — study anyway" else "Start study")
                    }
                }
                // Difficulty sits in the card's top-right corner, so it stays put on wide screens.
                Surface(
                    shape = CircleShape,
                    color = difficultyColor(difficulty).copy(alpha = 0.28f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        difficulty.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Text(
            "Everything up to ${maxLevel.code} is included. Change the level in Settings.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.6f),
        )

        // Grammar Deck (D58): browse-only reference, sits below the vocabulary deck.
        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenGrammar),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "§",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Grammar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$grammarCount concepts up to ${maxLevel.code} · levels are separate decks",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.65f),
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, modifier = Modifier.alpha(0.5f))
            }
        }
    }
}
/** My Progress (D17): words seen · correct · needing repetition. */
@Composable
fun ProgressScreen(repository: ProgressRepository?, entries: List<Entry>) {
    val total = entries.size
    val seen = repository?.seenCount() ?: 0L
    val needing = repository?.flaggedCount() ?: 0L
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("My Progress", style = MaterialTheme.typography.headlineMedium)
        StatRow("Words in dataset", "$total")
        StatRow("Cards seen", "$seen")
        StatRow("Needing repetition", "$needing")    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Settings (D45/D39): cumulative level slider, daily new-Card allowance, theme. */
@Composable
fun SettingsScreen(
    repository: ProgressRepository?,
    maxLevel: Level,
    onMaxLevelChange: (Level) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    var newPerDay by remember { mutableStateOf(repository?.newPerDay() ?: DEFAULT_NEW_PER_DAY) }
    val sliderIndex = SupportedLevels.indexOf(maxLevel).coerceAtLeast(0).toFloat()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Level", style = MaterialTheme.typography.titleMedium)
        Text(
            "Everything up to and including ${maxLevel.code} is included " +
                "(${SupportedLevels.take(sliderIndex.toInt() + 1).joinToString(" + ") { it.code }})",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.7f),
        )
        Slider(
            value = sliderIndex,
            onValueChange = { value ->
                val index = value.roundToInt().coerceIn(0, SupportedLevels.lastIndex)
                onMaxLevelChange(SupportedLevels[index])
            },
            valueRange = 0f..SupportedLevels.lastIndex.toFloat(),
            steps = (SupportedLevels.size - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SupportedLevels.forEach { level ->
                val reached = level.ordinal <= maxLevel.ordinal
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        level.code,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.alpha(if (level == maxLevel) 1f else 0.6f),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("New cards per day", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Introduced each day", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    newPerDay = (newPerDay - 5).coerceAtLeast(5)
                    repository?.setNewPerDay(newPerDay)
                },
            ) { Text("\u2212") }
            Text("$newPerDay", style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = {
                    newPerDay = (newPerDay + 5).coerceAtMost(100)
                    repository?.setNewPerDay(newPerDay)
                },
            ) { Text("+") }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark theme", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Switch(checked = darkTheme, onCheckedChange = { onToggleTheme() })
        }
    }
}

/** Attribution (ADR-0002 / ADR-0005): the sources and licences this app is built on. */
@Composable
fun AttributionsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Attribution", style = MaterialTheme.typography.headlineMedium)

        Attribution(
            title = "german-cefr-wordlist & FrequencyWords",
            detail = "The level-tagged headwords (A1–B1) come from ph98/german-cefr-wordlist, " +
                "which maps the FrequencyWords German list into approximate CEFR bands. " +
                "Levels are frequency-estimated, not official CEFR. MIT licence.",
            links = listOf("github.com/ph98/german-cefr-wordlist", "github.com/hermitdave/FrequencyWords"),
        )
        Attribution(
            title = "Kaikki.org / Wiktextract",
            detail = "Article/gender, plural, verb forms, IPA and example sentences come from the " +
                "open German dictionary extract. Licensed CC BY-SA 4.0 — attribution required, " +
                "share-alike.",
            links = listOf("kaikki.org/dictionary/German/"),
        )
        Attribution(
            title = "Wiktionary",
            detail = "Upstream source of the Kaikki extract. Text available under CC BY-SA 4.0 " +
                "(and GFDL).",
            links = listOf("de.wiktionary.org"),
        )
        Attribution(
            title = "Tatoeba",
            detail = "Example sentences (up to 5 per word) and the accusative/dative usage " +
                "examples come from Tatoeba sentence pairs. Licensed CC BY 2.0 FR — attribution required.",
            links = listOf("tatoeba.org"),
        )
        Attribution(
            title = "OdeNet — Open German WordNet",
            detail = "Related words come from OdeNet's synonym groups. " +
                "Licensed CC BY-SA 4.0 — attribution and share-alike.",
            links = listOf("github.com/hdaSprachtechnologie/odenet"),
        )
        Attribution(
            title = "Universal Dependencies — German GSD",
            detail = "Fallback accusative/dative usage examples. Licensed CC BY-SA 4.0 — " +
                "attribution and share-alike.",
            links = listOf("universaldependencies.org/treebanks/de_gsd"),
        )
        Attribution(
            title = "Grammar references",
            detail = "Grammar concepts are written originally; topic coverage and factual checks " +
                "used Wikibooks German and Wikipedia's German declension articles (CC BY-SA). " +
                "See grammar.json for per-concept source references.",
            links = listOf("en.wikibooks.org/wiki/German", "en.wikipedia.org/wiki/German_declension"),
        )
        Attribution(
            title = "Poppins",
            detail = "The app's typeface. Licensed under the SIL Open Font License 1.1.",
            links = listOf("fonts.google.com/specimen/Poppins"),
        )
        Attribution(
            title = "Open-source libraries",
            detail = "Kotlin, Compose Multiplatform (JetBrains), SQLDelight (Cash App), " +
                "kotlinx-datetime, kotlinx-serialization — Apache License 2.0.",
            links = emptyList(),
        )
    }
}

@Composable
private fun Attribution(title: String, detail: String, links: List<String>) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.8f))
            links.forEach { link ->
                Text(
                    link,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Review Deck (D36): Cards the learner rated "Again", kept for focused re-practice. */
@Composable
fun ReviewDeckScreen(
    repository: ProgressRepository?,
    entries: List<Entry>,
    onStudy: (List<StudyCard>) -> Unit,
) {
    val byId = remember(entries) { entries.associateBy { it.id } }
    val reviewCards = remember(repository, entries) {
        repository?.flaggedCards().orEmpty().mapNotNull { progress ->
            byId[progress.entryId]?.let { entry -> StudyCard(entry, progress.mode) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (reviewCards.isEmpty()) "Nothing to review"
            else "${reviewCards.size} card${if (reviewCards.size == 1) "" else "s"} to review",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (reviewCards.isEmpty()) {
            Text(
                "Cards you rate \"Again\" during study will collect here.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Button(onClick = { onStudy(reviewCards) }, modifier = Modifier.fillMaxWidth()) {
                Text("Study Review Deck")
            }
            reviewCards.forEach { card ->
                Text(
                    "${card.entry.displayForm} — ${card.entry.translations.firstOrNull().orEmpty()} " +
                        "(${card.mode.label})",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/**
 * Test (D37): a shuffled set of Cards the learner has *already seen* (reps > 0), taken across the
 * selected levels. Starts the same flip-and-rate flow as deck/review study, without the daily cap.
 */
@Composable
fun TestScreen(
    repository: ProgressRepository?,
    entries: List<Entry>,
    onStudy: (List<StudyCard>) -> Unit,
) {
    val byId = remember(entries) { entries.associateBy { it.id } }
    val testCards = remember(repository, entries) {
        repository?.allCards().orEmpty()
            .filter { it.reps > 0 }
            .mapNotNull { progress ->
                byId[progress.entryId]?.let { entry -> StudyCard(entry, progress.mode) }
            }
            .shuffled()
            .take(10)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Test", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Random cards from the words you've already seen",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (testCards.isEmpty()) {
            Text(
                "Nothing to test yet. Study a deck first — the Test uses cards you've already seen.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Button(onClick = { onStudy(testCards) }, modifier = Modifier.fillMaxWidth()) {
                Text("Start test (${testCards.size} cards)")
            }
            testCards.forEach { card ->
                Text(
                    "${card.entry.displayForm} — ${card.entry.translations.firstOrNull().orEmpty()} " +
                        "(${card.mode.label})",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
