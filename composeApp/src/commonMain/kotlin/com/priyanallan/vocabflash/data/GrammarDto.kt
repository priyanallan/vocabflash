package com.priyanallan.vocabflash.data

import com.priyanallan.vocabflash.domain.ExampleSentence
import com.priyanallan.vocabflash.domain.GrammarBlock
import com.priyanallan.vocabflash.domain.GrammarCategoryInfo
import com.priyanallan.vocabflash.domain.GrammarConcept
import com.priyanallan.vocabflash.domain.GrammarContent
import com.priyanallan.vocabflash.domain.GrammarSource
import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val GRAMMAR_PATH = "files/grammar.json"

private val grammarJson = Json { ignoreUnknownKeys = true }

/** Wire format of the bundled `grammar.json` (D60/D61). Hand-authored, versioned. */
@Serializable
data class GrammarFileDto(
    val schemaVersion: Int = 1,
    val contentVersion: String = "dev",
    val levels: List<String> = emptyList(),
    val categories: List<GrammarCategoryDto> = emptyList(),
    val sourceReferences: List<GrammarSourceDto> = emptyList(),
    val concepts: List<GrammarConceptDto> = emptyList(),
)

@Serializable
data class GrammarCategoryDto(val id: String, val title: String, val order: Int)

@Serializable
data class GrammarSourceDto(
    val id: String,
    val title: String,
    val url: String,
    val license: String,
    val usage: String? = null,
)

@Serializable
data class GrammarConceptDto(
    val id: String,
    val level: String,
    val category: String,
    val title: String,
    val sourceRefs: List<String> = emptyList(),
    val snippet: List<GrammarBlockDto> = emptyList(),
)

/** Polymorphic blocks; the `type` field in JSON picks the variant. */
@Serializable
sealed interface GrammarBlockDto {
    @Serializable
    @SerialName("table")
    data class TableDto(        val columns: List<String> = emptyList(),
        val rows: List<TableRowDto> = emptyList(),
    ) : GrammarBlockDto

    @Serializable
    data class TableRowDto(val label: String, val cells: List<String>)

    @Serializable
    @SerialName("pattern")
    data class PatternDto(
        val label: String,
        val template: String,
        val meaning: String? = null,
    ) : GrammarBlockDto

    @Serializable
    @SerialName("examples")
    data class ExamplesDto(val items: List<ExampleDto> = emptyList()) : GrammarBlockDto

    @Serializable
    @SerialName("note")
    data class NoteDto(val text: String) : GrammarBlockDto
}

@OptIn(ExperimentalResourceApi::class)
suspend fun loadBundledGrammar(): GrammarContent = runCatching {
    val text = Res.readBytes(GRAMMAR_PATH).decodeToString()
    grammarJson.decodeFromString<GrammarFileDto>(text).toDomain()
}.getOrElse { GrammarContent() }

internal fun GrammarFileDto.toDomain(): GrammarContent = GrammarContent(
    contentVersion = contentVersion,
    categories = categories.map { GrammarCategoryInfo(it.id, it.title, it.order) },
    sources = sourceReferences.map { dto ->
        GrammarSource(dto.id, dto.title, dto.url, dto.license, dto.usage.orEmpty())
    },
    concepts = concepts.mapNotNull { it.toDomain() },
)

internal fun GrammarConceptDto.toDomain(): GrammarConcept? {
    val levelValue = Level.entries.firstOrNull { it.code.equals(level, ignoreCase = true) }
        ?: return null
    return GrammarConcept(
        id = id,
        level = levelValue,
        category = category,
        title = title,
        snippet = snippet.map { it.toDomain() },
        sourceRefs = sourceRefs,
    )
}

internal fun GrammarBlockDto.toDomain(): GrammarBlock = when (this) {
    is GrammarBlockDto.TableDto -> GrammarBlock.Table(
        columns = columns,
        rows = rows.map { GrammarBlock.Table.TableRow(label = it.label, cells = it.cells) },
    )
    is GrammarBlockDto.PatternDto -> GrammarBlock.Pattern(label, template, meaning)
    is GrammarBlockDto.ExamplesDto -> GrammarBlock.Examples(
        items.map { ExampleSentence(de = it.de, en = it.en, source = it.source) },
    )
    is GrammarBlockDto.NoteDto -> GrammarBlock.Note(text)
}
