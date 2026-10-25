package com.priyanallan.vocabflash

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.priyanallan.vocabflash.data.SampleEntries
import com.priyanallan.vocabflash.data.loadBundledDataset
import com.priyanallan.vocabflash.data.loadBundledGrammar
import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.GrammarContent
import com.priyanallan.vocabflash.domain.GrammarConcept
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.StudyCard
import com.priyanallan.vocabflash.domain.SupportedLevels
import com.priyanallan.vocabflash.domain.studyCards
import com.priyanallan.vocabflash.platform.PlatformBackHandler
import com.priyanallan.vocabflash.db.ProgressRepository
import com.priyanallan.vocabflash.theme.KlarrBackground
import com.priyanallan.vocabflash.theme.KlarrTheme
import com.priyanallan.vocabflash.ui.AttributionsScreen
import com.priyanallan.vocabflash.ui.DeckListScreen
import com.priyanallan.vocabflash.ui.GrammarConceptScreen
import com.priyanallan.vocabflash.ui.GrammarGridScreen
import com.priyanallan.vocabflash.ui.ProgressScreen
import com.priyanallan.vocabflash.ui.ReviewDeckScreen
import com.priyanallan.vocabflash.ui.SettingsScreen
import com.priyanallan.vocabflash.ui.StudyScreen
import com.priyanallan.vocabflash.ui.TestScreen


/** Destinations. `Decks` is the home screen (D33). */
sealed interface Screen {
    data object Decks : Screen
    data class Study(val level: Level) : Screen
    data object Review : Screen
    data object ReviewStudy : Screen
    data object Test : Screen
    data object TestStudy : Screen
    data object Progress : Screen
    data object Settings : Screen
    data object Attributions : Screen

    /** Grammar Deck home: the concept grid (D58). */
    data object Grammar : Screen

    /** One Grammar Concept enlarged full-screen; back returns to [Grammar] (D58). */
    data class GrammarDetail(val conceptId: String) : Screen
}

@Composable
fun App(repository: ProgressRepository? = null) {
    var darkTheme by remember { mutableStateOf(false) }

    // Load the bundled wordlist→Kaikki Dataset; falls back to SampleEntries if absent (ADR-0005).
    val entries by produceState(initialValue = SampleEntries) { value = loadBundledDataset() }
    // Grammar Deck content (D58–D60); empty content renders an "unavailable" state.
    val grammar by produceState(initialValue = GrammarContent()) { value = loadBundledGrammar() }
    KlarrTheme(darkTheme = darkTheme) {
        KlarrBackground(darkTheme = darkTheme) {
            AppRoot(
                entries = entries,
                grammar = grammar,
                darkTheme = darkTheme,
                repository = repository,
                onToggleTheme = { darkTheme = !darkTheme },
            )
        }
    }}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    entries: List<Entry>,
    grammar: GrammarContent,
    darkTheme: Boolean,
    repository: ProgressRepository?,
    onToggleTheme: () -> Unit,
) {    var screen by remember { mutableStateOf<Screen>(Screen.Decks) }
    var menuOpen by remember { mutableStateOf(false) }
    var reviewCards by remember { mutableStateOf<List<StudyCard>>(emptyList()) }
    var testCards by remember { mutableStateOf<List<StudyCard>>(emptyList()) }
    var maxLevel by remember(repository) {
        mutableStateOf(repository?.maxLevel() ?: SupportedLevels.last())
    }
    val visibleEntries = remember(entries, maxLevel) {
        entries.filter { it.level.ordinal <= maxLevel.ordinal }
    }
    // Used by Word Details to open a related word without disturbing the study queue (D51/Q4).
    val entryByLemma = remember(entries) { entries.associateBy { it.lemma.lowercase() } }

    // Back target: a grammar detail goes back to the grammar grid; anything else to Decks (D50).
    val backTarget: Screen = if (screen is Screen.GrammarDetail) Screen.Grammar else Screen.Decks
    PlatformBackHandler(enabled = screen !is Screen.Decks) { screen = backTarget }
    val grammarById = remember(grammar) { grammar.concepts.associateBy { it.id } }
    // Scroll positions are hoisted above the `when` so they survive leaving a screen's
    // composition: the grid keeps its offset across detail round-trips, and each concept
    // keeps its own reading position (keyed by concept id).
    // Grammar levels are separate decks; entering selects the configured max level first.
    var grammarLevel by remember { mutableStateOf(maxLevel) }
    val grammarGridState = remember { LazyGridState() }
    val grammarDetailScrollStates = remember { mutableStateMapOf<String, ScrollState>() }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(titleFor(screen)) },
                navigationIcon = {
                    if (screen !is Screen.Decks) {
                        IconButton(onClick = { screen = backTarget }) { Text("\u2190") }
                    }
                },                actions = {
                    // The ⋮ menu is only offered on Home; other screens have their own back arrow.
                    if (screen is Screen.Decks) {
                        IconButton(onClick = { menuOpen = true }) { Text("\u22EE") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Grammar") },
                                onClick = { menuOpen = false; screen = Screen.Grammar },
                            )
                            DropdownMenuItem(
                                text = { Text("My Progress") },                                onClick = { menuOpen = false; screen = Screen.Progress },
                            )
                            DropdownMenuItem(
                                text = { Text("Review") },
                                onClick = { menuOpen = false; screen = Screen.Review },
                            )
                            DropdownMenuItem(
                                text = { Text("Test") },
                                onClick = { menuOpen = false; screen = Screen.Test },
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { menuOpen = false; screen = Screen.Settings },
                            )
                            DropdownMenuItem(
                                text = { Text("Attribution") },
                                onClick = { menuOpen = false; screen = Screen.Attributions },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = screen) {
                is Screen.Decks -> DeckListScreen(
                    entries = visibleEntries,
                    maxLevel = maxLevel,
                    repository = repository,
                    grammarCount = grammar.concepts.count { it.level.ordinal <= maxLevel.ordinal },
                    onStart = { screen = Screen.Study(maxLevel) },
                    onOpenGrammar = { grammarLevel = maxLevel; screen = Screen.Grammar },
                )
                is Screen.Grammar -> GrammarGridScreen(
                    content = grammar,
                    selectedLevel = grammarLevel,
                    onSelectLevel = { level -> grammarLevel = level },
                    gridState = grammarGridState,
                    onOpenConcept = { concept -> screen = Screen.GrammarDetail(concept.id) },
                )
                is Screen.GrammarDetail -> {
                    val concept = grammarById[s.conceptId]
                    if (concept != null) {
                        GrammarConceptScreen(
                            concept = concept,
                            content = grammar,
                            scrollState = grammarDetailScrollStates.getOrPut(s.conceptId) { ScrollState(0) },
                        )
                    } else {
                        Text("Concept not found.", modifier = Modifier.padding(20.dp))
                    }
                }
                is Screen.Study -> StudyScreen(
                    cards = studyCards(visibleEntries),
                    repository = repository,
                    lookupEntry = { word -> entryByLemma[word.lowercase()] },
                    onDone = { screen = Screen.Decks },
                )
                is Screen.Review -> ReviewDeckScreen(
                    repository = repository,
                    entries = entries,
                    onStudy = { cards ->
                        reviewCards = cards
                        screen = Screen.ReviewStudy
                    },
                )
                is Screen.ReviewStudy -> StudyScreen(
                    cards = reviewCards,
                    repository = repository,
                    applyDailyLimit = false,
                    lookupEntry = { word -> entryByLemma[word.lowercase()] },
                    onDone = { screen = Screen.Review },
                )
                is Screen.Test -> TestScreen(
                    repository = repository,
                    entries = visibleEntries,
                    onStudy = { cards ->
                        testCards = cards
                        screen = Screen.TestStudy
                    },
                )
                is Screen.TestStudy -> StudyScreen(
                    cards = testCards,
                    repository = repository,
                    applyDailyLimit = false,
                    lookupEntry = { word -> entryByLemma[word.lowercase()] },
                    onDone = { screen = Screen.Test },
                )
                is Screen.Progress -> ProgressScreen(repository, entries)
                is Screen.Settings -> SettingsScreen(
                    repository = repository,
                    maxLevel = maxLevel,
                    onMaxLevelChange = { level ->
                        maxLevel = level
                        repository?.setMaxLevel(level)
                    },
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                )
                is Screen.Attributions -> AttributionsScreen()
            }
        }
    }
}

private fun titleFor(screen: Screen): String = when (screen) {
    is Screen.Decks -> "Vocab Flash"
    is Screen.Study -> "${screen.level.code} Deck"
    is Screen.Grammar -> "Grammar"
    is Screen.GrammarDetail -> "Grammar"
    is Screen.Review -> "Review"
    is Screen.ReviewStudy -> "Review"
    is Screen.Test -> "Test"
    is Screen.TestStudy -> "Test"
    is Screen.Progress -> "My Progress"
    is Screen.Settings -> "Settings"
    is Screen.Attributions -> "Attribution"
}
