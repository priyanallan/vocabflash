package com.priyanallan.vocabflash.data

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parse guard for the bundled Grammar Deck content (D60/D61).
 * Reads the hand-authored grammar.json directly from the source tree (JVM unit test) so a
 * schema/SerialName/discriminator mismatch fails here instead of silently emptying the deck.
 */
class GrammarJsonTest {
    private val file = File("src/commonMain/composeResources/files/grammar.json")

    @Test
    fun `grammar json parses and yields concepts`() {
        val text = file.readText()
        val dto = Json { ignoreUnknownKeys = true }
            .decodeFromString<GrammarFileDto>(text)

        // 29 A1 + 14 A2 + 9 B1 — see docs/grammar-concept-list.md
        assertEquals(52, dto.concepts.size)
        assertEquals(29, dto.concepts.count { it.level == "A1" })
        assertEquals(14, dto.concepts.count { it.level == "A2" })
        assertEquals(9, dto.concepts.count { it.level == "B1" })
        assertTrue(dto.concepts.all { it.snippet.isNotEmpty() }, "empty snippet")
        assertEquals(52, dto.concepts.map { it.id }.toSet().size, "concept ids must be unique")
    }
}
