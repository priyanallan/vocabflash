package app.vocabflash.datasetbuilder.kaikki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Inflected-form entries (erste, alten, …) carry "inflection of …" glosses instead of meanings.
 * Those glosses must never surface as translations; the referenced base lemma is captured instead
 * so the enricher can fall back to the base word's real gloss.
 */
class KaikkiParserTest {

    private fun parse(jsonText: String): CompactKaikki =
        KaikkiParser.parse("erste", Json.parseToJsonElement(jsonText).jsonObject).base

    @Test
    fun inflectionGlossesAreNotTranslations() {
        val entry = parse(
            """{"word":"erste","pos":"num",
                "senses":[{"glosses":["inflection of erste.: strong/mixed nominative masculine singular, strong genitive/dative feminine singular, strong genitive plural"],
                           "tags":["form-of"]}]}""",
        )
        assertEquals(emptyList(), entry.translations)
        assertEquals("erste", entry.inflectionBase)
    }

    @Test
    fun alternativeAndObsoleteSpellingGlossesAreAlsoNotTranslations() {
        val entry = parse(
            """{"word":"Ablaß","pos":"noun",
                "senses":[{"glosses":["obsolete spelling of Abfluss"]}]}""",
        )
        assertEquals(emptyList(), entry.translations)
        assertEquals("Abfluss", entry.inflectionBase)
    }

    @Test
    fun realGlossesStillBecomeTranslations() {
        val entry = parse(
            """{"word":"erste","pos":"adj",
                "senses":[{"glosses":["first (1st)"]}]}""",
        )
        assertEquals(listOf("first"), entry.translations) // existing behaviour: parenthetical trimmed
        assertNull(entry.inflectionBase)
    }

    @Test
    fun formOfSenseTagsDoNotImplyGender() {
        val entry = parse(
            """{"word":"erster","pos":"num",
                "senses":[{"tags":["form-of","masculine","nominative","singular","strong"],
                           "glosses":["inflection of erste:", "strong/mixed nominative masculine singular"]}]}""",
        )
        assertNull(entry.gender)
    }

    @Test
    fun realNounSenseTagsStillYieldGender() {
        val entry = parse(
            """{"word":"Mann","pos":"noun",
                "senses":[{"tags":["masculine"],"glosses":["adult male person"]}]}""",
        )
        assertEquals("masculine", entry.gender)
    }

    @Test
    fun quotedMeaningInFormOfGlossIsExtracted() {
        val entry = parse(
            """{"word":"alle","pos":"pron",
                "senses":[{"tags":["form-of"],"glosses":["inflection of all (“all”)"]}]}""",
        )
        assertEquals(listOf("all"), entry.translations)
        assertEquals("all", entry.inflectionBase)
    }

    @Test
    fun meaningAfterColonInFormOfGlossIsExtracted() {
        val entry = parse(
            """{"word":"dem","pos":"article",
                "senses":[{"tags":["dative","form-of","masculine","singular"],
                           "glosses":["dative masculine/neuter singular of der: the"]}]}""",
        )
        assertEquals(listOf("the"), entry.translations)
        assertEquals("der", entry.inflectionBase)
    }

    @Test
    fun comparativeDegreeGlossYieldsTheRealComparative() {
        val entry = parse(
            """{"word":"besser","pos":"adj",
                "senses":[{"tags":["comparative","form-of"],
                           "glosses":["comparative degree of gut; better"]}]}""",
        )
        assertEquals(listOf("better"), entry.translations)
    }

    @Test
    fun mixedGlossesKeepOnlyRealMeanings() {
        val entry = parse(
            """{"word":"erstes","pos":"adj",
                "senses":[{"glosses":["inflection of erster","first"]}]}""",
        )
        assertEquals(listOf("first"), entry.translations)
        assertEquals("erster", entry.inflectionBase)
    }
}
