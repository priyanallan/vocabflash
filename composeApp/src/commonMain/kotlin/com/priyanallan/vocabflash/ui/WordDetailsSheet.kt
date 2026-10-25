package com.priyanallan.vocabflash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priyanallan.vocabflash.domain.CaseUsage
import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.ExampleSentence
import com.priyanallan.vocabflash.domain.RelatedRelation
import com.priyanallan.vocabflash.domain.RelatedWord
import com.priyanallan.vocabflash.domain.readableExamples

private val LABEL_COLUMN = 104.dp

/**
 * Word Details (D51): a scrim + bottom sheet. Sections are separated by dividers and laid out for
 * scanning — examples as bullets, related words and case usage as label/value tables.
 * Sections with no data are omitted rather than shown empty.
 */
@Composable
fun WordDetailsSheet(
    entry: Entry,
    onClose: () -> Unit,
    onOpenRelated: (String) -> Unit,
) {
    val examples = entry.readableExamples()
    val hasContent = entry.related.isNotEmpty() ||
        examples.isNotEmpty() ||
        entry.caseUsages.isNotEmpty() ||
        entry.conjugation != null ||
        entry.principalParts.isNotEmpty() ||
        entry.plural != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                Header(entry.displayForm, onClose)

                if (entry.related.isNotEmpty()) {
                    SectionDivider()
                    SectionTitle("Related words")
                    RelatedTable(entry.related, onOpenRelated)
                }

                if (examples.isNotEmpty()) {
                    SectionDivider()
                    SectionTitle("Examples")
                    ExampleBullets(examples)
                }

                if (entry.conjugation != null || entry.principalParts.isNotEmpty() || entry.plural != null) {
                    SectionDivider()
                    SectionTitle("Grammar")
                    VerbTable(entry)
                }

                if (entry.caseUsages.isNotEmpty()) {
                    SectionDivider()
                    SectionTitle("Case usage")
                    CaseTable(entry.caseUsages.take(2))
                }

                if (!hasContent) {
                    SectionDivider()
                    Text(
                        "No extra details for this word yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.7f),
                    )
                }

                Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(word: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "WORD DETAILS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(word, style = MaterialTheme.typography.headlineMedium)
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp).clickable(onClick = onClose),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("\u00D7", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 20.dp, bottom = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(10.dp))
}

/** Label/value table with a divider between rows. */
@Composable
private fun TableRow(
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showDivider: Boolean = false,
    value: @Composable () -> Unit,
) {
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            modifier = Modifier.width(LABEL_COLUMN).alpha(0.75f),
        )
        Box(Modifier.weight(1f)) { value() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedTable(words: List<RelatedWord>, onOpenRelated: (String) -> Unit) {
    val groups = RelatedRelation.entries.mapNotNull { relation ->
        words.filter { it.relation == relation }.takeIf { it.isNotEmpty() }?.let { relation to it }
    }
    Column {
        groups.forEachIndexed { index, (relation, group) ->
            TableRow(
                label = relation.label,
                showDivider = index > 0,
                value = {
                    // Each word is its own clickable chip, so taps open THAT word.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        group.forEach { related ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    related.word,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onOpenRelated(related.word) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ExampleBullets(examples: List<ExampleSentence>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        examples.forEach { example ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(16.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(example.de, style = MaterialTheme.typography.bodyLarge)
                    example.en?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.65f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerbTable(entry: Entry) {
    val parts = entry.principalParts
    val rows = buildList {
        entry.plural?.let { add("Plural" to it) }
        entry.conjugation?.presentDu?.let { add("du" to it) }
        entry.conjugation?.presentEr?.let { add("er / sie / es" to it) }
        entry.conjugation?.presentIhr?.let { add("ihr" to it) }
        parts.getOrNull(1)?.let { add("Präteritum" to it) }
        parts.getOrNull(2)?.let { add("Partizip II" to it) }
        entry.conjugation?.partizipI?.let { add("Partizip I" to it) }
    }
    Column {
        rows.forEachIndexed { index, (label, value) ->
            TableRow(
                label = label,
                showDivider = index > 0,
                value = { Text(value, style = MaterialTheme.typography.bodyLarge) },
            )
        }
    }
}

@Composable
private fun CaseTable(usages: List<CaseUsage>) {
    Column {
        usages.forEachIndexed { index, usage ->
            TableRow(
                label = usage.case.label,
                labelColor = MaterialTheme.colorScheme.primary,
                showDivider = index > 0,
                value = {
                    Column {
                        Text(usage.sentence, style = MaterialTheme.typography.bodyLarge)
                        usage.gloss?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.alpha(0.6f),
                            )
                        }
                    }
                },
            )
        }
    }
}
