package com.priyanallan.vocabflash.domain

/**
 * Grammar Deck model (D58–D63, ADR-0006): browse-only reference concepts, one snippet per
 * concept, rendered from data — never scheduled, never rated.
 */
data class GrammarConcept(
    val id: String,
    val level: Level,
    val category: String,
    val title: String,
    val snippet: List<GrammarBlock>,
    val sourceRefs: List<String> = emptyList(),
)

data class GrammarCategoryInfo(
    val id: String,
    val title: String,
    val order: Int,
)

data class GrammarSource(
    val id: String,
    val title: String,
    val url: String,
    val license: String,
    val usage: String,
)

/** The parsed contents of the bundled `grammar.json`. */
data class GrammarContent(
    val contentVersion: String = "dev",
    val categories: List<GrammarCategoryInfo> = emptyList(),
    val sources: List<GrammarSource> = emptyList(),
    val concepts: List<GrammarConcept> = emptyList(),
) {
    /** Concepts of exactly [level], grouped in category order — levels are separate decks
     *  (A2 shows only A2 concepts; no cumulative mixing), per the 2026-10-22 revision of D59. */
    fun groupsFor(level: Level): List<Pair<GrammarCategoryInfo, List<GrammarConcept>>> {
        val visible = concepts.filter { it.level == level }
        return categories
            .sortedBy { it.order }
            .map { cat -> cat to visible.filter { it.category == cat.id } }
            .filter { (_, list) -> list.isNotEmpty() }
    }
}
/**
 * One visual block of a cheat-sheet snippet. The shared renderer draws thumbnail and
 * full-screen views from the same blocks (D60).
 */
sealed interface GrammarBlock {
    data class Table(val columns: List<String>, val rows: List<TableRow>) : GrammarBlock {
        data class TableRow(val label: String, val cells: List<String>)
    }

    data class Pattern(
        val label: String,
        val template: String,
        val meaning: String? = null,
    ) : GrammarBlock

    data class Examples(val items: List<ExampleSentence>) : GrammarBlock

    data class Note(val text: String) : GrammarBlock
}

/** SourceReference ids used by concepts, resolved for attribution display. */
fun GrammarContent.sourcesFor(concept: GrammarConcept): List<GrammarSource> =
    concept.sourceRefs.mapNotNull { ref -> sources.firstOrNull { it.id == ref } }
