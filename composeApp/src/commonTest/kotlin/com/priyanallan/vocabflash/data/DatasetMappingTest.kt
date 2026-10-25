package com.priyanallan.vocabflash.data

import com.priyanallan.vocabflash.domain.Level
import com.priyanallan.vocabflash.domain.PartOfSpeech
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks that the pipeline's Dataset schema maps correctly onto the domain model. */
class DatasetMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun dto(jsonText: String): DatasetEntryDto =
        json.decodeFromString<DatasetEntryDto>(jsonText)

    @Test
    fun mapsVerbEntry() {
        val entry = dto(
            """{"id":"a1-essen-verb","lemma":"essen","level":"A1","pos":"verb",
               "verb":{"infinitive":"essen","praeteritum":"aß","partizipII":"gegessen"},
               "translations":["to eat"],"ipa":["/ˈɛsn̩/"],
               "examples":[{"de":"Wir essen Brot.","en":"We eat bread."}],"topic":"Food"}""",
        ).toDomain()!!

        assertEquals(Level.A1, entry.level)
        assertEquals(PartOfSpeech.Verb, entry.partOfSpeech)
        assertEquals(listOf("essen", "aß", "gegessen"), entry.principalParts)
        assertEquals("ˈɛsn̩", entry.ipa) // surrounding slashes stripped for display
        assertEquals("Wir essen Brot.", entry.examples.first().de)
        assertEquals("We eat bread.", entry.examples.first().en)
    }

    @Test
    fun mapsNounWithArticleAndPlural() {
        val entry = dto(
            """{"id":"a1-haus-noun","lemma":"Haus","level":"A1","pos":"noun",
               "article":"das","plural":"Häuser","translations":["house"]}""",
        ).toDomain()!!

        assertEquals(PartOfSpeech.Noun, entry.partOfSpeech)
        assertEquals("das Haus", entry.displayForm)
        assertEquals("Häuser", entry.plural)
    }

    @Test
    fun adjectiveMapsWithoutVerbOrArticle() {
        val entry = dto(
            """{"id":"a2-wichtig-adj","lemma":"wichtig","level":"A2","pos":"adj",
               "translations":["important"]}""",
        ).toDomain()!!

        assertEquals(PartOfSpeech.Adjective, entry.partOfSpeech)
        assertEquals("wichtig", entry.displayForm)
        assertEquals(emptyList(), entry.principalParts)
        assertTrue(entry.examples.isEmpty())
    }

    @Test
    fun unknownLevelIsDropped() {
        assertNull(dto("""{"id":"x","lemma":"x","level":"ZZ","pos":"noun"}""").toDomain())
    }
}
