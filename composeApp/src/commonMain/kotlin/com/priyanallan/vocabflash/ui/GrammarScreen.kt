package com.priyanallan.vocabflash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.launch
import com.priyanallan.vocabflash.domain.ExampleSentence
import com.priyanallan.vocabflash.domain.GrammarBlock
import com.priyanallan.vocabflash.domain.GrammarCategoryInfo
import com.priyanallan.vocabflash.domain.GrammarConcept
import com.priyanallan.vocabflash.domain.GrammarContent
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.SupportedLevels
import com.priyanallan.vocabflash.domain.sourcesFor

/**
 * Grammar Deck UI (D58–D60, ADR-0006).
 * Grid of concept thumbnails grouped by category; tapping opens the same snippet enlarged
 * full-screen. No ratings, no reveal state — the deck is a browse-only reference.
 */

/** Grammar home: a grid of concept snippets grouped by category.
 *  Levels are separate decks (A1/A2/B1 chips; each shows only its own concepts, per the
 *  2026-10-22 revision of D59). The filter button at the top right lists "Show all" plus
 *  every category of the selected level; picking one jumps to that section header. */
@Composable
fun GrammarGridScreen(
    content: GrammarContent,
    selectedLevel: Level,
    onSelectLevel: (Level) -> Unit,
    onOpenConcept: (GrammarConcept) -> Unit,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val groups = remember(content, selectedLevel) { content.groupsFor(selectedLevel) }
    val scope = rememberCoroutineScope()
    var filterMenuOpen by remember { mutableStateOf(false) }
    // Purely visual state: which section the last jump targeted (null = all sections).
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    // The grid renders, per group: one header item + N concept items; headers span both columns.
    val headerIndexById = remember(groups) {
        var index = 0
        groups.associate { (category, concepts) ->
            val headerIndex = index
            index += 1 + concepts.size
            category.id to headerIndex
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(Modifier.align(Alignment.CenterStart)) {
                SupportedLevels.forEach { level ->
                    val selected = level == selectedLevel
                    TextButton(onClick = { onSelectLevel(level) }) {
                        Text(
                            level.code,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                }
            }
            TextButton(
                onClick = { filterMenuOpen = true },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    selectedFilter?.let { id ->
                        groups.first { it.first.id == id }.first.title
                    } ?: "Filter",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(" ⌄", style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Show all") },
                    onClick = {
                        filterMenuOpen = false
                        selectedFilter = null
                        scope.launch { gridState.scrollToItem(0) }
                    },
                )
                groups.forEach { (category, _) ->
                    DropdownMenuItem(
                        text = { Text(category.title) },
                        onClick = {
                            filterMenuOpen = false
                            selectedFilter = category.id
                            scope.launch { gridState.scrollToItem(headerIndexById.getValue(category.id)) }
                        },
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {        for ((category, concepts) in groups) {
            item(span = { GridItemSpan(2) }, key = "header-${category.id}") {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(concepts, key = { it.id }) { concept ->
                GrammarConceptCard(
                    concept = concept,
                    onClick = { onOpenConcept(concept) },
                )
            }
        }
        }
    }
}
/** Grid thumbnail: title above, a compact cheat-sheet snippet below (D58). */
@Composable
private fun GrammarConceptCard(
    concept: GrammarConcept,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clickable(onClick = onClick),
    ) {
        Column {
            Text(
                concept.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            // Compact preview of the same data; the full view renders every block (D60).
            Box(Modifier.heightIn(max = 150.dp).clipToBounds()) {
                GrammarSnippetView(
                    blocks = concept.snippet.take(2),
                    compact = true,
                )
            }
        }
    }
}

/** Full-screen concept view: the whole snippet on one scrollable cheat-sheet card.
 *  [scrollState] is hoisted by the caller so the reading position survives navigating
 *  back to the grid and reopening the same concept. */
@Composable
fun GrammarConceptScreen(
    concept: GrammarConcept,
    content: GrammarContent,
    scrollState: ScrollState,
) {
    val sources = remember(concept, content) { content.sourcesFor(concept) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${concept.level.code} · ${concept.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
                GrammarSnippetView(blocks = concept.snippet, compact = false)
            }
        }
        if (sources.isNotEmpty()) {
            Text(
                "Sources: " + sources.joinToString { it.title },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

/** The one renderer for snippet blocks; [compact] is the grid-thumbnail style (D60). */
@Composable
internal fun GrammarSnippetView(
    blocks: List<GrammarBlock>,
    compact: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is GrammarBlock.Table -> GrammarTableView(table = block, compact = compact)
                is GrammarBlock.Pattern -> GrammarPatternView(pattern = block, compact = compact)
                is GrammarBlock.Examples -> GrammarExamplesView(items = block.items, compact = compact)
                is GrammarBlock.Note -> GrammarNoteView(text = block.text, compact = compact)
            }
        }
    }
}

@Composable
private fun GrammarTableView(
    table: GrammarBlock.Table,
    compact: Boolean,
    maxRows: Int? = null, // thumbnails cap rows instead of clipping mid-row
) {
    val headerStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
    val cellStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val headerBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    // Weight per column: the leading row-label column takes ~30%, the rest share the width.
    // Cell weights are shared by every row, so columns stay aligned across the table.
    val weights = table.columns.mapIndexed { index, _ -> if (index == 0) 0.3f else 0.7f / (table.columns.size - 1).coerceAtLeast(1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
    ) {
        Row(Modifier.fillMaxWidth().background(headerBackground)) {
            table.columns.forEachIndexed { index, column ->
                Text(
                    column,
                    style = headerStyle,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(weights[index])
                        .border(1.dp, borderColor)
                        .padding(6.dp),
                )
            }
        }
        val visibleRows = maxRows?.let { table.rows.take(it) } ?: table.rows
        visibleRows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    row.label,
                    style = cellStyle,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(weights[0])
                        .border(1.dp, borderColor)
                        .padding(6.dp),
                )
                row.cells.forEachIndexed { index, cell ->
                    Text(
                        cell,
                        style = cellStyle,
                        modifier = Modifier
                            .weight(weights[index + 1])
                            .border(1.dp, borderColor)
                            .padding(6.dp),
                    )
                }
            }
        }
        if (table.rows.size > visibleRows.size) {
            Text(
                "+ ${table.rows.size - visibleRows.size} more rows — open for the full table",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun GrammarPatternView(pattern: GrammarBlock.Pattern, compact: Boolean) {
    val style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyLarge
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(pattern.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(pattern.template, style = style, fontWeight = FontWeight.SemiBold)
        if (pattern.meaning != null) {
            Text(
                pattern.meaning,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun GrammarExamplesView(items: List<ExampleSentence>, compact: Boolean) {
    val deStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val enStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { example ->
            Column {
                Text(example.de, style = deStyle, fontWeight = FontWeight.Medium)
                if (example.en != null && !compact) {
                    Text(
                        example.en,
                        style = enStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GrammarNoteView(text: String, compact: Boolean) {
    Text(
        text,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .padding(8.dp),
    )
}
